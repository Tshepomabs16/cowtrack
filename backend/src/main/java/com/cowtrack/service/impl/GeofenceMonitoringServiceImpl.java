package com.cowtrack.service.impl;

import com.cowtrack.config.GeofenceProperties;
import com.cowtrack.entity.Cow;
import com.cowtrack.entity.Geofence;
import com.cowtrack.entity.GeofenceOccupancy;
import com.cowtrack.entity.LocationRecord;
import com.cowtrack.repository.GeofenceOccupancyRepository;
import com.cowtrack.repository.GeofenceRepository;
import com.cowtrack.service.AlertService;
import com.cowtrack.service.GeofenceMonitoringService;
import com.cowtrack.util.AlertMessageGenerator;
import com.cowtrack.util.FenceCrossing;
import com.cowtrack.util.FenceShapes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GeofenceMonitoringServiceImpl implements GeofenceMonitoringService {

    private final GeofenceRepository geofenceRepository;
    private final GeofenceOccupancyRepository occupancyRepository;
    private final AlertService alertService;
    private final AlertMessageGenerator alertMessageGenerator;
    private final GeofenceProperties properties;

    @Override
    public void evaluate(Long farmId, Cow cow, LocationRecord fix) {
        if (fix == null || fix.getLatitude() == null || fix.getLongitude() == null) {
            return;
        }
        for (Geofence fence : fencesApplyingTo(farmId, cow)) {
            // One unusable fence must not stop the others being checked.
            try {
                evaluate(farmId, cow, fence, fix);
            } catch (RuntimeException e) {
                log.error("Could not evaluate fence {} for cow {}: {}",
                        fence.getGeofenceId(), cow.getCowId(), e.getMessage());
            }
        }
    }

    /** The animal's own camp, if it is live, and every live restricted zone on the farm. */
    private List<Geofence> fencesApplyingTo(Long farmId, Cow cow) {
        List<Geofence> fences = new ArrayList<>();
        Geofence camp = cow.getCamp();
        if (camp != null && isLive(camp) && camp.isKeepIn()
                && camp.getFarm().getFarmId().equals(farmId)) {
            fences.add(camp);
        }
        fences.addAll(geofenceRepository.findLiveByFarmAndType(farmId, Geofence.FenceType.KEEP_OUT));
        return fences;
    }

    private void evaluate(Long farmId, Cow cow, Geofence fence, LocationRecord fix) {
        GeofenceOccupancy.Key key = new GeofenceOccupancy.Key(fence.getGeofenceId(), cow.getCowId());
        GeofenceOccupancy occupancy = occupancyRepository.findById(key).orElse(null);

        if (occupancy != null && !fix.getRecordedAt().isAfter(occupancy.getLastFixAt())) {
            return;
        }

        double signedDistance = FenceShapes.signedDistanceMeters(
                fence, fix.getLatitude().doubleValue(), fix.getLongitude().doubleValue());
        double accuracy = fix.getAccuracy() != null ? fix.getAccuracy().doubleValue() : 0;

        FenceCrossing.State previous = occupancy == null
                ? null
                : new FenceCrossing.State(occupancy.getIsInside(), occupancy.getPendingFixes());
        FenceCrossing.Outcome outcome = FenceCrossing.evaluate(fence.isKeepIn(), previous, signedDistance,
                accuracy, properties.getBoundaryBufferMetres(), properties.getConfirmFixes());

        if (occupancy == null) {
            occupancy = new GeofenceOccupancy();
            occupancy.setGeofenceId(fence.getGeofenceId());
            occupancy.setCowId(cow.getCowId());
        }
        occupancy.setIsInside(outcome.next().inside());
        occupancy.setPendingFixes(outcome.next().pendingFixes());
        occupancy.setLastFixAt(fix.getRecordedAt());
        occupancyRepository.save(occupancy);

        switch (outcome.event()) {
            case BREACH -> alertService.raiseGeofenceBreach(farmId, cow, fence, Math.abs(signedDistance));
            case CLEARED -> alertService.clearGeofenceBreach(farmId, cow.getCowId(), fence.getGeofenceId(),
                    alertMessageGenerator.generateFenceClearedNote(
                            fence.getName(), fence.isKeepIn(), fix.getRecordedAt()));
            case NONE -> {
            }
        }
    }

    private static boolean isLive(Geofence fence) {
        return Boolean.TRUE.equals(fence.getIsActive()) && fence.getRetiredAt() == null;
    }
}
