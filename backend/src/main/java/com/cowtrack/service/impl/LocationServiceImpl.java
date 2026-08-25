package com.cowtrack.service.impl;

import com.cowtrack.dto.request.LocationRequest;
import com.cowtrack.dto.response.LocationResponse;
import com.cowtrack.entity.*;
import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.repository.CowRepository;
import com.cowtrack.repository.GeofenceRepository;
import com.cowtrack.repository.LocationRecordRepository;
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
    private final AlertService alertService;
    private final LocationMapper locationMapper;
    private final GeofenceCalculator geofenceCalculator;

    @Override
    public LocationResponse recordLocation(LocationRequest request) {
        // Get cow
        Cow cow = cowRepository.findById(request.getCowId())
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + request.getCowId()));

        // Create location record
        LocationRecord location = locationMapper.toEntity(request);
        location.setCow(cow);

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
        List<LocationRecord> locations;
        if (limit != null && limit > 0) {
            locations = locationRecordRepository.findByCowCowIdOrderByRecordedAtDesc(cowId)
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        } else {
            locations = locationRecordRepository.findByCowCowIdOrderByRecordedAtDesc(cowId);
        }

        return locations.stream()
                .map(locationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LocationResponse getCurrentLocation(Long cowId) {
        LocationRecord location = locationRecordRepository.findLatestByCowId(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("No location found for cow id: " + cowId));
        return locationMapper.toResponse(location);
    }

    @Override
    public List<LocationResponse> getLocationsInTimeRange(Long cowId, LocalDateTime start, LocalDateTime end) {
        List<LocationRecord> locations = locationRecordRepository.findByCowIdAndTimeRange(cowId, start, end);
        return locations.stream()
                .map(locationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void checkGeofenceViolations(Long cowId) {
        // Get latest location
        LocationRecord latestLocation = locationRecordRepository.findLatestByCowId(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("No location found for cow id: " + cowId));

        // Get cow's geofence
        Geofence geofence = geofenceRepository.findByCowCowId(cowId).orElse(null);

        if (geofence == null) {
            log.debug("No geofence defined for cow {}", cowId);
            return;
        }

        // Check if location is inside geofence
        boolean isInside = geofenceCalculator.isInsideGeofence(
                latestLocation.getLatitude(),
                latestLocation.getLongitude(),
                geofence.getCenterLatitude(),
                geofence.getCenterLongitude(),
                geofence.getRadiusMeters()
        );

        // Get previous location to determine if this is an entry or exit
        List<LocationRecord> recentLocations = locationRecordRepository.findByCowCowIdOrderByRecordedAtDesc(cowId);
        if (recentLocations.size() > 1) {
            LocationRecord previousLocation = recentLocations.get(1);

            boolean wasInside = geofenceCalculator.isInsideGeofence(
                    previousLocation.getLatitude(),
                    previousLocation.getLongitude(),
                    geofence.getCenterLatitude(),
                    geofence.getCenterLongitude(),
                    geofence.getRadiusMeters()
            );

            // If status changed, create alert
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
        List<LocationRecord> locations = locationRecordRepository.findByCowIdAndTimeRange(cowId, start, end);

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

    private void checkOtherAlerts(Cow cow) {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        LocalDateTime now = LocalDateTime.now();

        // Check for no signal in last 24 hours
        List<LocationRecord> recentLocations = locationRecordRepository.findByCowIdAndTimeRange(
                cow.getCowId(), twentyFourHoursAgo, now);

        if (recentLocations.isEmpty()) {
            long hoursSinceLastSignal = 24;
            // Actually we should check the last recorded time
            LocationRecord lastLocation = locationRecordRepository.findLatestByCowId(cow.getCowId()).orElse(null);
            if (lastLocation != null) {
                hoursSinceLastSignal = java.time.Duration.between(lastLocation.getRecordedAt(), now).toHours();
            }

            if (hoursSinceLastSignal >= 24) {
                alertService.createNoSignalAlert(cow.getCowId(), hoursSinceLastSignal);
            }
        }

        // Check for night movement (between 10 PM and 5 AM)
        int currentHour = LocalDateTime.now().getHour();
        if (currentHour >= 22 || currentHour < 5) {
            // Check if cow has moved significantly in the last hour
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            List<LocationRecord> nightLocations = locationRecordRepository.findByCowIdAndTimeRange(
                    cow.getCowId(), oneHourAgo, now);

            if (nightLocations.size() >= 2) {
                // Calculate distance moved in last hour
                double distanceMoved = 0;
                for (int i = 1; i < nightLocations.size(); i++) {
                    distanceMoved += geofenceCalculator.calculateDistance(
                            nightLocations.get(i - 1).getLatitude(),
                            nightLocations.get(i - 1).getLongitude(),
                            nightLocations.get(i).getLatitude(),
                            nightLocations.get(i).getLongitude()
                    );
                }

                // If moved more than 100 meters at night, create alert
                if (distanceMoved > 100) {
                    // This would create a night movement alert
                    // alertService.createNightMovementAlert(cow.getCowId());
                    log.info("Night movement detected for cow {}: {} meters", cow.getTagId(), distanceMoved);
                }
            }
        }
    }
}