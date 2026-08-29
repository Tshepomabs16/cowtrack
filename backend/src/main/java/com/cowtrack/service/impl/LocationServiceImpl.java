package com.cowtrack.service.impl;

import com.cowtrack.dto.request.LocationRequest;
import com.cowtrack.dto.response.LocationResponse;
import com.cowtrack.entity.*;
import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.repository.CowRepository;
import com.cowtrack.repository.FarmRepository;
import com.cowtrack.repository.GeofenceRepository;
import com.cowtrack.repository.LocationRecordRepository;
import com.cowtrack.security.FarmContext;
import com.cowtrack.service.AlertService;
import com.cowtrack.service.LocationService;
import com.cowtrack.service.mapper.LocationMapper;
import com.cowtrack.util.GeofenceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LocationServiceImpl implements LocationService {

    private final LocationRecordRepository locationRecordRepository;
    private final CowRepository cowRepository;
    private final GeofenceRepository geofenceRepository;
    private final FarmRepository farmRepository;
    private final FarmContext farmContext;
    private final AlertService alertService;
    private final LocationMapper locationMapper;
    private final GeofenceCalculator geofenceCalculator;

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

        // Check for geofence violations
        checkGeofenceViolations(cow.getCowId());

        // Check for other alerts (no signal, night movement, etc.)
        checkOtherAlerts(cow);

        return locationMapper.toResponse(savedLocation);
    }

    @Override
    public List<LocationResponse> getLocationHistory(Long cowId, Integer limit) {
        Long farmId = farmContext.getCurrentFarmId();
        if (!cowRepository.findByFarmFarmIdAndCowId(farmId, cowId).isPresent()) {
            throw new ResourceNotFoundException("Cow not found with id: " + cowId);
        }
        List<LocationRecord> locations;
        if (limit != null && limit > 0) {
            locations = locationRecordRepository.findByFarmFarmIdAndCowCowIdOrderByRecordedAtDesc(farmId, cowId)
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        } else {
            locations = locationRecordRepository.findByFarmFarmIdAndCowCowIdOrderByRecordedAtDesc(farmId, cowId);
        }

        return locations.stream()
                .map(locationMapper::toResponse)
                .collect(Collectors.toList());
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
        LocationRecord latestLocation = locationRecordRepository.findLatestByFarmIdAndCowId(farmId, cowId)
                .orElseThrow(() -> new ResourceNotFoundException("No location found for cow id: " + cowId));

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

        List<LocationRecord> recentLocations = locationRecordRepository.findByFarmFarmIdAndCowCowIdOrderByRecordedAtDesc(farmId, cowId);
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
        Long farmId = farmContext.getCurrentFarmId();
        LocalDateTime now = LocalDateTime.now();

        int currentHour = LocalDateTime.now().getHour();
        if (currentHour >= 22 || currentHour < 5) {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            List<LocationRecord> nightLocations = locationRecordRepository.findByFarmIdAndCowIdAndTimeRange(
                    farmId, cow.getCowId(), oneHourAgo, now);

            if (nightLocations.size() >= 2) {
                double distanceMoved = 0;
                for (int i = 1; i < nightLocations.size(); i++) {
                    distanceMoved += geofenceCalculator.calculateDistance(
                            nightLocations.get(i - 1).getLatitude(),
                            nightLocations.get(i - 1).getLongitude(),
                            nightLocations.get(i).getLatitude(),
                            nightLocations.get(i).getLongitude()
                    );
                }

                if (distanceMoved > 100) {
                    log.info("Night movement detected for cow {}: {} meters", cow.getTagId(), distanceMoved);
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getLiveLocations() {
        Long farmId = farmContext.getCurrentFarmId();
        return cowRepository.findByFarmFarmId(farmId).stream()
                // Scoped by farm as well as cow: the cow list is already filtered,
                // but an unscoped latest-record lookup would still surface a record
                // written by another farm and display it on this farm's map.
                .map(cow -> locationRecordRepository
                        .findLatestByFarmIdAndCowId(farmId, cow.getCowId())
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(locationMapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }
}
