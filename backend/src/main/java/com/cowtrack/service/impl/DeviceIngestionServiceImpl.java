package com.cowtrack.service.impl;

import com.cowtrack.dto.request.DeviceBatchRequest;
import com.cowtrack.dto.request.DeviceReading;
import com.cowtrack.dto.response.IngestResult;
import com.cowtrack.entity.*;
import com.cowtrack.exception.BusinessException;
import com.cowtrack.repository.*;
import com.cowtrack.realtime.EventTypes;
import com.cowtrack.realtime.RealtimePublisher;
import com.cowtrack.security.DeviceAuthenticator;
import com.cowtrack.security.FarmContext;
import com.cowtrack.service.AlertService;
import com.cowtrack.service.DeviceIngestionService;
import com.cowtrack.service.LocationService;
import com.cowtrack.service.mapper.LocationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceIngestionServiceImpl implements DeviceIngestionService {

    /**
     * A reading dated further ahead than this is refused. Collars keep time
     * poorly, and one with a badly wrong clock would otherwise write a position
     * far in the future that permanently wins every "latest position" query —
     * freezing the live map and suppressing no-signal detection for that animal.
     */
    private static final Duration MAX_CLOCK_SKEW = Duration.ofHours(1);

    /** Older than this and the reading is of no operational use. */
    private static final Duration MAX_BACKFILL = Duration.ofDays(30);

    /**
     * Reading timestamps are truncated to this before storage and comparison.
     *
     * <p>LocalDateTime carries nanoseconds; the TIMESTAMP column does not keep
     * them. An exact-match duplicate check therefore never matched what had
     * actually been written, so a resent batch — which is routine, since a collar
     * resends anything it was not acknowledged for — silently duplicated the
     * animal's history. No collar samples faster than this.
     */
    private static final ChronoUnit READING_PRECISION = ChronoUnit.MILLIS;

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    private final DeviceAuthenticator deviceAuthenticator;
    private final DeviceRepository deviceRepository;
    private final LocationRecordRepository locationRepository;
    private final HealthMetricRepository healthMetricRepository;
    private final LocationService locationService;
    private final AlertService alertService;
    private final FarmContext farmContext;
    private final LocationMapper locationMapper;
    private final RealtimePublisher realtimePublisher;

    @Override
    @Transactional
    public IngestResult ingest(String serialNumber, String apiKey, DeviceBatchRequest batch) {
        Device device = deviceAuthenticator.authenticate(serialNumber, apiKey);

        Cow cow = device.getCow();
        if (cow == null) {
            // Provisioned but not fitted. Authenticating is allowed so a
            // technician can prove the collar works; readings are not, because a
            // position with no animal attached says nothing.
            throw new BusinessException("Device " + serialNumber + " is not assigned to an animal");
        }

        Farm farm = device.getFarm();
        IngestResult result = new IngestResult();

        recordDeviceState(device, batch, result);

        // The device authenticated by key, which establishes its farm. Acting as
        // that farm lets the ordinary services be reused rather than duplicated
        // with farm-explicit overloads.
        return farmContext.actingAsFarm(farm.getFarmId(), () -> {
            LocalDateTime latestBefore = locationRepository
                    .findLatestByFarmIdAndCowId(farm.getFarmId(), cow.getCowId())
                    .map(LocationRecord::getRecordedAt)
                    .orElse(null);

            // Oldest first, so a batch spanning a gap is stored in the order the
            // animal actually moved. The payload order is not trusted.
            List<DeviceReading> ordered = batch.getReadings().stream()
                    .sorted(Comparator.comparing(DeviceReading::getRecordedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();

            for (int i = 0; i < ordered.size(); i++) {
                store(ordered.get(i), i, cow, farm, result);
            }

            Optional<LocationRecord> newest = locationRepository
                    .findLatestByFarmIdAndCowId(farm.getFarmId(), cow.getCowId());
            LocalDateTime latestAfter = newest.map(LocationRecord::getRecordedAt).orElse(null);

            // Only evaluate when the animal's newest position actually moved on.
            // A backfill of older readings must not raise a geofence breach for a
            // boundary the animal crossed and came back from days ago.
            if (advanced(latestBefore, latestAfter)) {
                result.setAlertsEvaluated(true);

                // One push per batch, carrying where the animal is now, rather
                // than one per stored reading. A collar returning from a week out
                // of coverage uploads hundreds of readings at once; replaying all
                // of them would flood every open map with a track the farmer has
                // no use for, to arrive at the same marker position.
                newest.ifPresent(record -> realtimePublisher.publish(
                        farm.getFarmId(), EventTypes.LOCATION_UPDATE,
                        locationMapper.toResponse(record)));

                evaluateAlerts(cow);
            }

            return result;
        });
    }

    /** Battery, firmware and contact time, recorded whatever happens to the readings. */
    private void recordDeviceState(Device device, DeviceBatchRequest batch, IngestResult result) {
        boolean wasLow = device.isBatteryLow();

        if (batch.getBatteryPercent() != null) {
            device.setBatteryPercent(batch.getBatteryPercent());
            result.setBatteryPercent(batch.getBatteryPercent());
        }
        if (batch.getFirmwareVersion() != null && !batch.getFirmwareVersion().isBlank()) {
            device.setFirmwareVersion(batch.getFirmwareVersion());
        }
        device.setLastSeenAt(LocalDateTime.now());
        deviceRepository.save(device);

        // Raised on the transition only. A collar sitting at 5% uploads all day,
        // and repeating the alert every batch would bury everything else.
        if (device.isBatteryLow() && !wasLow && device.getCow() != null) {
            farmContext.actingAsFarm(device.getFarm().getFarmId(), () ->
                    alertService.createLowBatteryAlert(
                            device.getCow().getCowId(),
                            device.getSerialNumber(),
                            device.getBatteryPercent()));
        }
    }

    private void store(DeviceReading reading, int index, Cow cow, Farm farm, IngestResult result) {
        String rejection = validate(reading);
        if (rejection != null) {
            result.reject(index, rejection);
            return;
        }

        LocalDateTime recordedAt = reading.getRecordedAt().truncatedTo(READING_PRECISION);
        boolean storedAnything = false;

        if (reading.hasPosition()) {
            // A collar that does not get an acknowledgement resends the batch, so
            // the same sample legitimately arrives more than once.
            boolean exists = !locationRepository
                    .findByFarmIdAndCowIdAndTimeRange(farm.getFarmId(), cow.getCowId(),
                            recordedAt, recordedAt)
                    .isEmpty();

            if (exists) {
                result.setDuplicates(result.getDuplicates() + 1);
                return;
            }

            LocationRecord location = new LocationRecord();
            location.setCow(cow);
            location.setFarm(farm);
            location.setLatitude(reading.getLatitude());
            location.setLongitude(reading.getLongitude());
            location.setAccuracy(reading.getAccuracy());
            location.setRecordedAt(recordedAt);
            locationRepository.save(location);
            storedAnything = true;
        }

        if (reading.hasVitals()) {
            Optional<HealthMetric> existing = healthMetricRepository
                    .findByCow_CowIdOrderByRecordedAtDesc(cow.getCowId()).stream()
                    .filter(metric -> metric.getRecordedAt().equals(recordedAt))
                    .findFirst();

            if (existing.isEmpty()) {
                HealthMetric metric = new HealthMetric();
                metric.setCow(cow);
                metric.setFarm(farm);
                metric.setTemperature(reading.getTemperature());
                metric.setHeartRate(reading.getHeartRate());
                metric.setActivityLevel(reading.getActivityLevel());
                metric.setRecordedAt(recordedAt);
                healthMetricRepository.save(metric);
                storedAnything = true;
            } else if (!reading.hasPosition()) {
                result.setDuplicates(result.getDuplicates() + 1);
                return;
            }
        }

        if (storedAnything) {
            result.setAccepted(result.getAccepted() + 1);
        } else {
            result.setDuplicates(result.getDuplicates() + 1);
        }
    }

    /** @return the reason to refuse this reading, or null if it is usable */
    private String validate(DeviceReading reading) {
        if (reading.getRecordedAt() == null) {
            return "Missing recordedAt";
        }
        // Checked before the "carries nothing" case below: a reading with only a
        // latitude satisfies neither hasPosition() nor hasVitals(), so the broader
        // message would fire first and hide the actual device bug.
        if ((reading.getLatitude() == null) != (reading.getLongitude() == null)) {
            return "Latitude and longitude must be supplied together";
        }
        if (!reading.hasPosition() && !reading.hasVitals()) {
            return "Reading carries neither a position nor any vital sign";
        }

        LocalDateTime now = LocalDateTime.now();
        if (reading.getRecordedAt().isAfter(now.plus(MAX_CLOCK_SKEW))) {
            return "Timestamp is in the future; check the device clock";
        }
        if (reading.getRecordedAt().isBefore(now.minus(MAX_BACKFILL))) {
            return "Timestamp is older than the accepted backfill window";
        }

        if (reading.hasPosition()) {
            if (outside(reading.getLatitude(), MIN_LATITUDE, MAX_LATITUDE)) {
                return "Latitude out of range";
            }
            if (outside(reading.getLongitude(), MIN_LONGITUDE, MAX_LONGITUDE)) {
                return "Longitude out of range";
            }
        }

        return null;
    }

    private boolean outside(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value.compareTo(min) < 0 || value.compareTo(max) > 0;
    }

    private boolean advanced(LocalDateTime before, LocalDateTime after) {
        if (after == null) {
            return false;
        }
        return before == null || after.isAfter(before);
    }

    /**
     * One animal's bad batch must not abort another's, and an alerting failure
     * must not discard readings that were successfully stored.
     */
    private void evaluateAlerts(Cow cow) {
        try {
            locationService.checkGeofenceViolations(cow.getCowId());
        } catch (RuntimeException e) {
            log.error("Alert evaluation failed for cow {} after ingestion: {}",
                    cow.getCowId(), e.getMessage());
        }
    }
}
