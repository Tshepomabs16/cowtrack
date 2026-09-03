package com.cowtrack.service.impl;

import com.cowtrack.config.NightMovementProperties;
import com.cowtrack.dto.request.LocationRequest;
import com.cowtrack.dto.response.LiveCowResponse;
import com.cowtrack.dto.response.LocationResponse;
import com.cowtrack.entity.*;
import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.repository.AlertRepository;
import com.cowtrack.repository.CowRepository;
import com.cowtrack.repository.FarmRepository;
import com.cowtrack.repository.GeofenceRepository;
import com.cowtrack.repository.LocationRecordRepository;
import com.cowtrack.realtime.EventTypes;
import com.cowtrack.realtime.RealtimePublisher;
import com.cowtrack.security.FarmContext;
import com.cowtrack.service.AlertService;
import com.cowtrack.service.LocationService;
import com.cowtrack.service.mapper.LocationMapper;
import com.cowtrack.util.GeofenceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LocationServiceImpl implements LocationService {

    /** Positions returned when the caller does not say how many it wants. */
    private static final int DEFAULT_HISTORY_LIMIT = 100;

    /** Ceiling on what a caller may ask for in one request. */
    private static final int MAX_HISTORY_LIMIT = 1000;


    private final LocationRecordRepository locationRecordRepository;
    private final CowRepository cowRepository;
    private final GeofenceRepository geofenceRepository;
    private final FarmRepository farmRepository;
    private final FarmContext farmContext;
    private final AlertService alertService;
    private final LocationMapper locationMapper;
    private final GeofenceCalculator geofenceCalculator;
    private final NightMovementProperties nightMovementProperties;
    private final RealtimePublisher realtimePublisher;
    private final AlertRepository alertRepository;

    @Override
    public LocationResponse recordLocation(LocationRequest request) {
        Long farmId = farmContext.getCurrentFarmId();

        // Scoped to the caller's farm. An unscoped findById here allowed one farm
        // to post positions onto another farm's animals, which both corrupted the
        // victim's tracking history and drove geofence evaluation on cattle the
        // caller has no claim to.
        Cow cow = cowRepository.findByFarmFarmIdAndCowId(farmId, request.getCowId())
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + request.getCowId()));

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));

        LocationRecord location = locationMapper.toEntity(request);
        location.setCow(cow);
        location.setFarm(farm);

        LocationRecord savedLocation = locationRecordRepository.save(location);
        log.info("Location recorded for cow {}: {}, {}", cow.getTagId(), request.getLatitude(), request.getLongitude());

        LocationResponse response = locationMapper.toResponse(savedLocation);
        realtimePublisher.publish(farmId, EventTypes.LOCATION_UPDATE, response);

        // Check for geofence violations
        checkGeofenceViolations(cow.getCowId());

        // Check for other alerts (no signal, night movement, etc.)
        checkOtherAlerts(cow);

        return response;
    }

    @Override
    public List<LocationResponse> getLocationHistory(Long cowId, Integer limit) {
        Long farmId = farmContext.getCurrentFarmId();
        if (!cowRepository.findByFarmFarmIdAndCowId(farmId, cowId).isPresent()) {
            throw new ResourceNotFoundException("Cow not found with id: " + cowId);
        }

        // Bounded by the database rather than by trimming the list afterwards.
        // This used to load the animal's whole history to return the newest
        // hundred rows of it, which is unbounded work that grows every time a
        // collar reports.
        List<LocationRecord> locations = locationRecordRepository.findRecentByFarmIdAndCowId(
                farmId, cowId, PageRequest.of(0, historyLimit(limit)));

        return locations.stream()
                .map(locationMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * An absent or nonsensical limit gets the default rather than everything.
     * "No limit" is not a safe reading of a missing parameter on a table that
     * grows for as long as the animal is alive.
     */
    private int historyLimit(Integer requested) {
        if (requested == null || requested <= 0) {
            return DEFAULT_HISTORY_LIMIT;
        }
        return Math.min(requested, MAX_HISTORY_LIMIT);
    }

    @Override
    public LocationResponse getCurrentLocation(Long cowId) {
        Long farmId = farmContext.getCurrentFarmId();
        if (!cowRepository.findByFarmFarmIdAndCowId(farmId, cowId).isPresent()) {
            throw new ResourceNotFoundException("Cow not found with id: " + cowId);
        }
        LocationRecord location = locationRecordRepository.findLatestByFarmIdAndCowId(farmId, cowId)
                .orElseThrow(() -> new ResourceNotFoundException("No location found for cow id: " + cowId));
        return locationMapper.toResponse(location);
    }

    @Override
    public List<LocationResponse> getLocationsInTimeRange(Long cowId, LocalDateTime start, LocalDateTime end) {
        Long farmId = farmContext.getCurrentFarmId();
        if (!cowRepository.findByFarmFarmIdAndCowId(farmId, cowId).isPresent()) {
            throw new ResourceNotFoundException("Cow not found with id: " + cowId);
        }
        List<LocationRecord> locations = locationRecordRepository.findByFarmIdAndCowIdAndTimeRange(farmId, cowId, start, end);
        return locations.stream()
                .map(locationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void checkGeofenceViolations(Long cowId) {
        Long farmId = farmContext.getCurrentFarmId();

        // A breach is a crossing, so it takes the newest position and the one
        // before it — two rows, not the animal's entire history. This ran on
        // every incoming position and loaded everything the collar had ever
        // reported to look at element 1 of it.
        List<LocationRecord> recentLocations = locationRecordRepository.findRecentByFarmIdAndCowId(
                farmId, cowId, PageRequest.of(0, 2));

        if (recentLocations.isEmpty()) {
            throw new ResourceNotFoundException("No location found for cow id: " + cowId);
        }
        LocationRecord latestLocation = recentLocations.get(0);

        Geofence geofence = geofenceRepository.findByCowCowId(cowId).orElse(null);

        if (geofence == null) {
            log.debug("No geofence defined for cow {}", cowId);
            return;
        }

        boolean isInside = geofenceCalculator.isInsideGeofence(
                latestLocation.getLatitude(),
                latestLocation.getLongitude(),
                geofence.getCenterLatitude(),
                geofence.getCenterLongitude(),
                geofence.getRadiusMeters()
        );

        if (recentLocations.size() > 1) {
            LocationRecord previousLocation = recentLocations.get(1);

            boolean wasInside = geofenceCalculator.isInsideGeofence(
                    previousLocation.getLatitude(),
                    previousLocation.getLongitude(),
                    geofence.getCenterLatitude(),
                    geofence.getCenterLongitude(),
                    geofence.getRadiusMeters()
            );

            if (wasInside != isInside) {
                alertService.createGeofenceBreachAlert(cowId, isInside);
                log.warn("Geofence {} detected for cow {}: {} -> {}",
                        isInside ? "entry" : "exit",
                        cowId,
                        wasInside ? "inside" : "outside",
                        isInside ? "inside" : "outside");
            }
        }
    }

    @Override
    public double calculateDistanceTraveled(Long cowId, LocalDateTime start, LocalDateTime end) {
        Long farmId = farmContext.getCurrentFarmId();
        List<LocationRecord> locations = locationRecordRepository.findByFarmIdAndCowIdAndTimeRange(farmId, cowId, start, end);

        if (locations.size() < 2) {
            return 0.0;
        }

        double totalDistance = 0.0;
        for (int i = 1; i < locations.size(); i++) {
            LocationRecord prev = locations.get(i - 1);
            LocationRecord curr = locations.get(i);

            totalDistance += geofenceCalculator.calculateDistance(
                    prev.getLatitude(),
                    prev.getLongitude(),
                    curr.getLatitude(),
                    curr.getLongitude()
            );
        }

        return totalDistance;
    }

    /**
     * Checks that genuinely belong on the arrival of a position.
     *
     * <p>No-signal detection used to live here and could never fire: it asked
     * whether any location had arrived recently, having just been called because
     * one did. Absence of a signal cannot be observed from the signal, so it now
     * runs on a timer in
     * {@link com.cowtrack.service.CollarMonitoringService}.
     */
    private void checkOtherAlerts(Cow cow) {
        checkNightMovement(cow);
    }

    /**
     * Sustained movement in the small hours, which is the signature of an animal
     * being driven off rather than grazing.
     *
     * <p>Correctly event-driven, unlike the no-signal check that used to sit
     * beside it: movement can only be seen in positions that arrive. A collar
     * that goes silent instead is the sweep's problem.
     */
    private void checkNightMovement(Cow cow) {
        if (!nightMovementProperties.isEnabled() || !isNightNow()) {
            return;
        }

        Long farmId = farmContext.getCurrentFarmId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minus(nightMovementProperties.getWindow());

        List<LocationRecord> nightLocations = locationRecordRepository.findByFarmIdAndCowIdAndTimeRange(
                farmId, cow.getCowId(), windowStart, now);

        if (nightLocations.size() < 2) {
            return;
        }

        // Path length, not displacement: an animal driven in a circle has still
        // been driven. The query returns newest-first, which does not matter to
        // a sum of absolute distances.
        double distanceMoved = 0;
        for (int i = 1; i < nightLocations.size(); i++) {
            distanceMoved += geofenceCalculator.calculateDistance(
                    nightLocations.get(i - 1).getLatitude(),
                    nightLocations.get(i - 1).getLongitude(),
                    nightLocations.get(i).getLatitude(),
                    nightLocations.get(i).getLongitude()
            );
        }

        if (distanceMoved > nightMovementProperties.getThresholdMetres()) {
            alertService.createNightMovementAlert(cow.getCowId(), distanceMoved, currentNightStart());
        }
    }

    private boolean isNightNow() {
        int hour = LocalDateTime.now(nightMovementProperties.getZone()).getHour();
        int start = nightMovementProperties.getStartHour();
        int end = nightMovementProperties.getEndHour();

        // The window normally wraps midnight (22:00 to 05:00), so the two cases
        // are not the same comparison.
        return start > end
                ? hour >= start || hour < end
                : hour >= start && hour < end;
    }

    /**
     * When the current night window opened, used to decide whether an existing
     * open alert belongs to tonight. Before the end hour we are past midnight, so
     * the window opened yesterday.
     */
    private LocalDateTime currentNightStart() {
        LocalDateTime local = LocalDateTime.now(nightMovementProperties.getZone());
        int start = nightMovementProperties.getStartHour();

        LocalDateTime tonight = local.withHour(start).withMinute(0).withSecond(0).withNano(0);
        return local.getHour() < start ? tonight.minusDays(1) : tonight;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LiveCowResponse> getLiveLocations() {
        Long farmId = farmContext.getCurrentFarmId();

        // Two queries for the whole herd, whatever its size. This used to walk the
        // cow list and run a latest-position lookup per animal, so opening the map
        // cost a query per head — imperceptible on a demo herd, seconds on a real
        // one.
        //
        // Still farm-scoped, and that is not incidental: the predicate inside the
        // subquery is what stops another farm's record being selected as this
        // farm's newest position, which is how an injected position once appeared
        // on the wrong map.
        List<LocationRecord> latest = locationRecordRepository.findLatestPerCowForFarm(farmId);
        Set<Long> withOpenAlerts = new HashSet<>(alertRepository.findCowIdsWithOpenAlerts(farmId));

        List<LiveCowResponse> live = new ArrayList<>();
        Long previousCowId = null;

        for (LocationRecord record : latest) {
            Cow cow = record.getCow();
            // Two records can tie on the newest timestamp; the ordering makes the
            // first of them the one to keep, so an animal gets one marker.
            if (cow.getCowId().equals(previousCowId)) {
                continue;
            }
            previousCowId = cow.getCowId();

            LiveCowResponse response = new LiveCowResponse();
            response.setLocationId(record.getLocationId());
            response.setCowId(cow.getCowId());
            response.setCowName(cow.getName());
            response.setTagId(cow.getTagId());
            response.setLatitude(record.getLatitude());
            response.setLongitude(record.getLongitude());
            response.setAccuracy(record.getAccuracy());
            response.setRecordedAt(record.getRecordedAt());
            response.setStatus(deriveStatus(
                    withOpenAlerts.contains(cow.getCowId()), record.getRecordedAt()));

            live.add(response);
        }
        return live;
    }

    /**
     * The same rule the cattle list uses, over data already loaded here.
     *
     * <p>Kept in step with {@code CowMapper.deriveStatus} by hand, which is a
     * seam worth watching: the two exist separately because one works from a cow
     * and its related records and this one works from a position that is already
     * in memory.
     */
    private String deriveStatus(boolean hasOpenAlerts, LocalDateTime recordedAt) {
        if (hasOpenAlerts) {
            return "alert";
        }
        if (recordedAt == null || recordedAt.isBefore(LocalDateTime.now().minusHours(24))) {
            return "inactive";
        }
        return "healthy";
    }
}
