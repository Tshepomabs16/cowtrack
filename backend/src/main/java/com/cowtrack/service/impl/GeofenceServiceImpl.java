package com.cowtrack.service.impl;

import com.cowtrack.dto.request.GeofenceRequest;
import com.cowtrack.dto.response.GeofenceResponse;
import com.cowtrack.entity.Cow;
import com.cowtrack.entity.Geofence;
import com.cowtrack.exception.BusinessException;
import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.entity.Farm;
import com.cowtrack.repository.CowRepository;
import com.cowtrack.repository.FarmRepository;
import com.cowtrack.repository.GeofenceRepository;
import com.cowtrack.security.FarmContext;
import com.cowtrack.service.GeofenceService;
import com.cowtrack.util.GeofenceUtils;
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
    private final FarmContext farmContext;
    private final FarmRepository farmRepository;

    @Override
    public GeofenceResponse createGeofence(GeofenceRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        Cow cow = cowRepository.findByFarmFarmIdAndCowId(farmId, request.getCowId())
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + request.getCowId()));

        // Check if cow already has a geofence
        geofenceRepository.findByCowCowId(request.getCowId())
                .ifPresent(g -> {
                    throw new BusinessException("Cow already has a geofence");
                });

        // Create geofence
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with id: " + farmId));

        Geofence geofence = new Geofence();
        geofence.setFarm(farm);
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
        Long farmId = farmContext.getCurrentFarmId();
        if (!geofence.getFarm().getFarmId().equals(farmId)) {
            throw new ResourceNotFoundException("Geofence not found for cow id: " + cowId);
        }
        return toResponse(geofence);
    }

    @Override
    public GeofenceResponse updateGeofence(Long geofenceId, GeofenceRequest request) {
        Geofence geofence = geofenceRepository.findById(geofenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Geofence not found with id: " + geofenceId));
        Long farmId = farmContext.getCurrentFarmId();
        if (!geofence.getFarm().getFarmId().equals(farmId)) {
            throw new ResourceNotFoundException("Geofence not found with id: " + geofenceId);
        }

        // If changing cow, check if new cow already has a geofence
        if (!geofence.getCow().getCowId().equals(request.getCowId())) {
            Cow newCow = cowRepository.findByFarmFarmIdAndCowId(farmId, request.getCowId())
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
        Geofence geofence = geofenceRepository.findById(geofenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Geofence not found with id: " + geofenceId));
        Long farmId = farmContext.getCurrentFarmId();
        if (!geofence.getFarm().getFarmId().equals(farmId)) {
            throw new ResourceNotFoundException("Geofence not found with id: " + geofenceId);
        }
        geofenceRepository.deleteById(geofenceId);
        log.info("Deleted geofence with id: {}", geofenceId);
    }

    @Override
    public List<GeofenceResponse> getGeofencesByCaretaker(Long caretakerId) {
        Long farmId = farmContext.getCurrentFarmId();
        List<Geofence> geofences = geofenceRepository.findByFarmFarmId(farmId);
        return geofences.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isLocationInsideGeofence(Long cowId, java.math.BigDecimal latitude, java.math.BigDecimal longitude) {
        Long farmId = farmContext.getCurrentFarmId();
        return geofenceRepository.findByCowCowId(cowId)
                .filter(geofence -> geofence.getFarm() != null && geofence.getFarm().getFarmId().equals(farmId))
                .map(geofence -> {
                    // For simplicity, we'll assume it's always active
                    // In real implementation, check isActive field
                    return GeofenceUtils.isInsideRadius(
                            latitude.doubleValue(),
                            longitude.doubleValue(),
                            geofence.getCenterLatitude().doubleValue(),
                            geofence.getCenterLongitude().doubleValue(),
                            geofence.getRadiusMeters()
                    );
                })
                .orElse(false);
    }

    @Override
    public GeofenceResponse deactivateGeofence(Long geofenceId) {
        Geofence geofence = geofenceRepository.findById(geofenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Geofence not found with id: " + geofenceId));
        Long farmId = farmContext.getCurrentFarmId();
        if (!geofence.getFarm().getFarmId().equals(farmId)) {
            throw new ResourceNotFoundException("Geofence not found with id: " + geofenceId);
        }

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
        Long farmId = farmContext.getCurrentFarmId();
        if (!geofence.getFarm().getFarmId().equals(farmId)) {
            throw new ResourceNotFoundException("Geofence not found with id: " + geofenceId);
        }

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
}