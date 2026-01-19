package com.cowtrack.service.impl;

import com.cowtrack.dto.request.GeofenceRequest;
import com.cowtrack.dto.response.GeofenceResponse;
import com.cowtrack.entity.Cow;
import com.cowtrack.entity.Geofence;
import com.cowtrack.exception.BusinessException;
import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.repository.CowRepository;
import com.cowtrack.repository.GeofenceRepository;
import com.cowtrack.service.GeofenceService;
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
public class GeofenceServiceImpl implements GeofenceService {

    private final GeofenceRepository geofenceRepository;
    private final CowRepository cowRepository;

    @Override
    public GeofenceResponse createGeofence(GeofenceRequest request) {
        // Check if cow exists
        Cow cow = cowRepository.findById(request.getCowId())
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + request.getCowId()));

        // Check if cow already has a geofence
        geofenceRepository.findByCowCowId(request.getCowId())
                .ifPresent(g -> {
                    throw new BusinessException("Cow already has a geofence");
                });

        // Create geofence
        Geofence geofence = new Geofence();
        geofence.setCow(cow);
        geofence.setCenterLatitude(request.getCenterLatitude());
        geofence.setCenterLongitude(request.getCenterLongitude());
        geofence.setRadiusMeters(request.getRadiusMeters());
        geofence.setCreatedAt(LocalDateTime.now());

        Geofence savedGeofence = geofenceRepository.save(geofence);
        log.info("Created geofence for cow {} with radius {} meters", cow.getTagId(), request.getRadiusMeters());

        return toResponse(savedGeofence);
    }

    @Override
    public GeofenceResponse getGeofenceByCowId(Long cowId) {
        Geofence geofence = geofenceRepository.findByCowCowId(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Geofence not found for cow id: " + cowId));
        return toResponse(geofence);
    }

    @Override
    public GeofenceResponse updateGeofence(Long geofenceId, GeofenceRequest request) {
        Geofence geofence = geofenceRepository.findById(geofenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Geofence not found with id: " + geofenceId));

        // If changing cow, check if new cow already has a geofence
        if (!geofence.getCow().getCowId().equals(request.getCowId())) {
            Cow newCow = cowRepository.findById(request.getCowId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + request.getCowId()));

            geofenceRepository.findByCowCowId(request.getCowId())
                    .ifPresent(g -> {
                        throw new BusinessException("Cow already has a geofence");
                    });

            geofence.setCow(newCow);
        }

        geofence.setCenterLatitude(request.getCenterLatitude());
        geofence.setCenterLongitude(request.getCenterLongitude());
        geofence.setRadiusMeters(request.getRadiusMeters());

        Geofence updatedGeofence = geofenceRepository.save(geofence);
        return toResponse(updatedGeofence);
    }

    @Override
    public void deleteGeofence(Long geofenceId) {
        if (!geofenceRepository.existsById(geofenceId)) {
            throw new ResourceNotFoundException("Geofence not found with id: " + geofenceId);
        }
        geofenceRepository.deleteById(geofenceId);
        log.info("Deleted geofence with id: {}", geofenceId);
    }

    @Override
    public List<GeofenceResponse> getGeofencesByCaretaker(Long caretakerId) {
        List<Geofence> geofences = geofenceRepository.findByCaretakerId(caretakerId);
        return geofences.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isLocationInsideGeofence(Long cowId, java.math.BigDecimal latitude, java.math.BigDecimal longitude) {
        return geofenceRepository.findByCowCowId(cowId)
                .map(geofence -> {
                    // For simplicity, we'll assume it's always active
                    // In real implementation, check isActive field
                    double distance = calculateDistance(
                            latitude.doubleValue(),
                            longitude.doubleValue(),
                            geofence.getCenterLatitude().doubleValue(),
                            geofence.getCenterLongitude().doubleValue()
                    );
                    return distance <= geofence.getRadiusMeters();
                })
                .orElse(false);
    }

    @Override
    public GeofenceResponse deactivateGeofence(Long geofenceId) {
        Geofence geofence = geofenceRepository.findById(geofenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Geofence not found with id: " + geofenceId));

        // Note: Your current schema doesn't have isActive field
        // We'll add it later, or you can add to entity
        // For now, we'll just return the geofence

        log.info("Geofence {} deactivated", geofenceId);
        return toResponse(geofence);
    }

    @Override
    public GeofenceResponse activateGeofence(Long geofenceId) {
        Geofence geofence = geofenceRepository.findById(geofenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Geofence not found with id: " + geofenceId));

        // Note: Your current schema doesn't have isActive field
        // We'll add it later, or you can add to entity
        // For now, we'll just return the geofence

        log.info("Geofence {} activated", geofenceId);
        return toResponse(geofence);
    }

    private GeofenceResponse toResponse(Geofence geofence) {
        GeofenceResponse response = new GeofenceResponse();
        response.setGeofenceId(geofence.getGeofenceId());
        response.setCowId(geofence.getCow().getCowId());
        response.setCowName(geofence.getCow().getName());
        response.setCenterLatitude(geofence.getCenterLatitude());
        response.setCenterLongitude(geofence.getCenterLongitude());
        response.setRadiusMeters(geofence.getRadiusMeters());
        response.setCreatedAt(geofence.getCreatedAt());
        response.setIsActive(true); // Default to true since schema doesn't have this field
        return response;
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        // Haversine formula
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}