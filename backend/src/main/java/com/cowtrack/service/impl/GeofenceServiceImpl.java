package com.cowtrack.service.impl;

import com.cowtrack.dto.request.GeofenceRequest;
import com.cowtrack.dto.response.GeofenceDeletionResult;
import com.cowtrack.dto.response.GeofenceResponse;
import com.cowtrack.entity.Cow;
import com.cowtrack.entity.Farm;
import com.cowtrack.entity.Geofence;
import com.cowtrack.exception.BusinessException;
import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.exception.ValidationException;
import com.cowtrack.repository.AlertRepository;
import com.cowtrack.repository.CowRepository;
import com.cowtrack.repository.FarmRepository;
import com.cowtrack.repository.GeofenceOccupancyRepository;
import com.cowtrack.repository.GeofenceRepository;
import com.cowtrack.security.FarmContext;
import com.cowtrack.service.AlertService;
import com.cowtrack.service.GeofenceService;
import com.cowtrack.util.FenceShapes;
import com.cowtrack.util.GeofenceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GeofenceServiceImpl implements GeofenceService {

    /** Larger than any single camp; a bigger circle is almost certainly a typo in metres. */
    private static final int MAX_RADIUS_METERS = 50_000;

    private final GeofenceRepository geofenceRepository;
    private final GeofenceOccupancyRepository occupancyRepository;
    private final CowRepository cowRepository;
    private final AlertRepository alertRepository;
    private final AlertService alertService;
    private final FarmContext farmContext;
    private final FarmRepository farmRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceResponse> getGeofences(boolean includeRetired) {
        Long farmId = farmContext.getCurrentFarmId();
        return geofenceRepository.findByFarmFarmId(farmId).stream()
                .filter(fence -> includeRetired || fence.getRetiredAt() == null)
                .sorted(Comparator.comparing(Geofence::getGeofenceId).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GeofenceResponse getGeofence(Long geofenceId) {
        return toResponse(findOnFarm(geofenceId));
    }

    @Override
    @Transactional(readOnly = true)
    public GeofenceResponse getCampForCow(Long cowId) {
        Cow cow = findCow(cowId);
        if (cow.getCamp() == null) {
            throw new ResourceNotFoundException("Cow " + cowId + " is not in a camp");
        }
        return toResponse(cow.getCamp());
    }

    @Override
    public GeofenceResponse createGeofence(GeofenceRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with id: " + farmId));

        Geofence fence = new Geofence();
        fence.setFarm(farm);
        apply(fence, request);
        fence.setIsActive(request.getIsActive() == null || request.getIsActive());
        fence.setCreatedAt(LocalDateTime.now());
        Geofence saved = geofenceRepository.save(fence);
        log.info("Created {} '{}' on farm {}", saved.getFenceType(), saved.getName(), farmId);

        if (request.getCowIds() != null && !request.getCowIds().isEmpty()) {
            return assignAnimals(saved.getGeofenceId(), request.getCowIds());
        }
        return toResponse(saved);
    }

    @Override
    public GeofenceResponse updateGeofence(Long geofenceId, GeofenceRequest request) {
        Geofence fence = findOnFarm(geofenceId);
        requireNotRetired(fence);

        Geofence.FenceType newType = parseFenceType(request.getFenceType());
        if (fence.isKeepIn() && newType == Geofence.FenceType.KEEP_OUT) {
            requireEmpty(fence, "turned into a restricted zone");
        }

        apply(fence, request);
        if (request.getIsActive() != null && !request.getIsActive().equals(fence.getIsActive())) {
            if (request.getIsActive()) {
                fence.setIsActive(true);
            } else {
                requireEmpty(fence, "switched off");
                switchOff(fence, "'" + fence.getName() + "' was switched off");
            }
        }
        fence.setUpdatedAt(LocalDateTime.now());
        return toResponse(geofenceRepository.save(fence));
    }

    @Override
    public GeofenceDeletionResult deleteGeofence(Long geofenceId) {
        Geofence fence = findOnFarm(geofenceId);
        requireEmpty(fence, "removed");

        if (alertRepository.existsByGeofenceGeofenceId(geofenceId)) {
            if (fence.getRetiredAt() == null) {
                switchOff(fence, "'" + fence.getName() + "' was removed");
                fence.setRetiredAt(LocalDateTime.now());
                fence.setUpdatedAt(LocalDateTime.now());
                geofenceRepository.save(fence);
                log.info("Retired fence {} rather than deleting it: it has raised alerts", geofenceId);
            }
            return new GeofenceDeletionResult(geofenceId, false, true,
                    "This fence has raised alerts, so it was retired rather than deleted to keep that history");
        }

        occupancyRepository.deleteByGeofenceId(geofenceId);
        geofenceRepository.delete(fence);
        log.info("Deleted fence {}", geofenceId);
        return new GeofenceDeletionResult(geofenceId, true, false, null);
    }

    @Override
    public GeofenceResponse deactivateGeofence(Long geofenceId) {
        Geofence fence = findOnFarm(geofenceId);
        requireNotRetired(fence);
        // Switching off a camp with animals in it would silently stop watching
        // them. Moving them out first makes that a decision rather than a side
        // effect.
        requireEmpty(fence, "switched off");
        if (Boolean.TRUE.equals(fence.getIsActive())) {
            switchOff(fence, "'" + fence.getName() + "' was switched off");
            fence.setUpdatedAt(LocalDateTime.now());
            geofenceRepository.save(fence);
            log.info("Fence {} switched off", geofenceId);
        }
        return toResponse(fence);
    }

    @Override
    public GeofenceResponse activateGeofence(Long geofenceId) {
        Geofence fence = findOnFarm(geofenceId);
        requireNotRetired(fence);
        if (!Boolean.TRUE.equals(fence.getIsActive())) {
            fence.setIsActive(true);
            fence.setUpdatedAt(LocalDateTime.now());
            geofenceRepository.save(fence);
            log.info("Fence {} switched on", geofenceId);
        }
        return toResponse(fence);
    }

    @Override
    public GeofenceResponse assignAnimals(Long campId, List<Long> cowIds) {
        Long farmId = farmContext.getCurrentFarmId();
        Geofence camp = findOnFarm(campId);
        requireNotRetired(camp);
        if (!camp.isKeepIn()) {
            throw new BusinessException("'" + camp.getName() + "' is a restricted zone; animals are put in camps");
        }
        if (!Boolean.TRUE.equals(camp.getIsActive())) {
            throw new BusinessException("'" + camp.getName() + "' is switched off; switch it on before moving animals in");
        }

        // Resolved up front so an unknown id refuses the whole move rather than
        // half of it.
        List<Cow> cows = new ArrayList<>();
        for (Long cowId : cowIds) {
            cows.add(findCow(cowId));
        }

        for (Cow cow : cows) {
            Geofence previous = cow.getCamp();
            if (previous != null && previous.getGeofenceId().equals(campId)) {
                continue;
            }
            if (previous != null) {
                leaveCamp(farmId, cow, previous, "Moved to camp '" + camp.getName() + "'");
            }
            // A fresh baseline in the new camp: the first position there is
            // judged on its own, not against where the animal was before.
            occupancyRepository.deleteByGeofenceIdAndCowId(campId, cow.getCowId());
            cow.setCamp(camp);
            cowRepository.save(cow);
        }
        log.info("Moved {} animal(s) into camp {}", cows.size(), campId);
        return toResponse(camp);
    }

    @Override
    public GeofenceResponse removeAnimal(Long campId, Long cowId) {
        Long farmId = farmContext.getCurrentFarmId();
        Geofence camp = findOnFarm(campId);
        Cow cow = findCow(cowId);
        if (cow.getCamp() == null || !cow.getCamp().getGeofenceId().equals(campId)) {
            throw new ResourceNotFoundException("Cow " + cowId + " is not in camp " + campId);
        }
        leaveCamp(farmId, cow, camp, "Taken out of camp '" + camp.getName() + "'");
        cow.setCamp(null);
        cowRepository.save(cow);
        return toResponse(camp);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLocationInsideGeofence(Long cowId, BigDecimal latitude, BigDecimal longitude) {
        Cow cow = findCow(cowId);
        if (cow.getCamp() == null) {
            return false;
        }
        return FenceShapes.signedDistanceMeters(cow.getCamp(), latitude.doubleValue(), longitude.doubleValue()) <= 0;
    }

    /** An animal leaving a camp takes its open breach with it: it is no longer expected to be there. */
    private void leaveCamp(Long farmId, Cow cow, Geofence camp, String note) {
        alertService.clearGeofenceBreach(farmId, cow.getCowId(), camp.getGeofenceId(), note);
        occupancyRepository.deleteByGeofenceIdAndCowId(camp.getGeofenceId(), cow.getCowId());
    }

    /**
     * Stops evaluating a fence: open breaches against it are closed, since
     * nothing will now close them, and its state is forgotten so that switching
     * it back on starts from a fresh baseline.
     */
    private void switchOff(Geofence fence, String note) {
        Long farmId = fence.getFarm().getFarmId();
        alertRepository.findByFarmFarmIdAndIsResolvedFalseOrderByCreatedAtDesc(farmId).stream()
                .filter(alert -> alert.getGeofence() != null
                        && alert.getGeofence().getGeofenceId().equals(fence.getGeofenceId()))
                .forEach(alert -> alertService.clearGeofenceBreach(
                        farmId, alert.getCow().getCowId(), fence.getGeofenceId(), note));
        occupancyRepository.deleteByGeofenceId(fence.getGeofenceId());
        fence.setIsActive(false);
    }

    private void requireEmpty(Geofence fence, String action) {
        long animals = cowRepository.countByCampGeofenceId(fence.getGeofenceId());
        if (animals > 0) {
            throw new BusinessException("'" + fence.getName() + "' still has " + animals
                    + (animals == 1 ? " animal" : " animals")
                    + " in it. Move them to another camp before it is " + action + ".");
        }
    }

    private void requireNotRetired(Geofence fence) {
        if (fence.getRetiredAt() != null) {
            throw new BusinessException("'" + fence.getName() + "' has been removed and can no longer be changed");
        }
    }

    private Geofence findOnFarm(Long geofenceId) {
        Long farmId = farmContext.getCurrentFarmId();
        return geofenceRepository.findByFarmFarmIdAndGeofenceId(farmId, geofenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Geofence not found with id: " + geofenceId));
    }

    private Cow findCow(Long cowId) {
        Long farmId = farmContext.getCurrentFarmId();
        return cowRepository.findByFarmFarmIdAndCowId(farmId, cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + cowId));
    }

    /** Validates the request as a whole and copies it onto the fence. */
    private void apply(Geofence fence, GeofenceRequest request) {
        Geofence.FenceType fenceType = parseFenceType(request.getFenceType());
        Geofence.Shape shape = parseShape(request);
        List<String> problems = new ArrayList<>();

        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty()) {
            problems.add("a name is required");
        }

        if (shape == Geofence.Shape.CIRCLE) {
            if (request.getCenterLatitude() == null || request.getCenterLongitude() == null) {
                problems.add("a circle needs a centre latitude and longitude");
            } else if (isNullIsland(request.getCenterLatitude(), request.getCenterLongitude())) {
                problems.add("the centre is at 0, 0, which is what a missing position looks like");
            }
            if (request.getRadiusMeters() == null || request.getRadiusMeters() <= 0) {
                problems.add("a circle needs a radius greater than zero");
            } else if (request.getRadiusMeters() > MAX_RADIUS_METERS) {
                problems.add("a radius over " + MAX_RADIUS_METERS + " m is larger than any camp");
            }
        } else {
            checkCorners(request.getVertices(), problems);
        }

        if (!problems.isEmpty()) {
            throw new ValidationException("Geofence is not valid: " + String.join("; ", problems));
        }

        fence.setName(name);
        fence.setDescription(request.getDescription());
        fence.setFenceType(fenceType);
        fence.setShape(shape);
        if (shape == Geofence.Shape.CIRCLE) {
            fence.setCenterLatitude(request.getCenterLatitude());
            fence.setCenterLongitude(request.getCenterLongitude());
            fence.setRadiusMeters(request.getRadiusMeters());
            fence.setVerticesJson(null);
        } else {
            fence.setCenterLatitude(null);
            fence.setCenterLongitude(null);
            fence.setRadiusMeters(null);
            fence.setVerticesJson(FenceShapes.toJson(request.getVertices()));
        }
    }

    private void checkCorners(List<List<BigDecimal>> vertices, List<String> problems) {
        if (vertices == null || vertices.size() < 3) {
            problems.add("a polygon needs at least three corners");
            return;
        }
        List<double[]> corners = new ArrayList<>();
        for (List<BigDecimal> vertex : vertices) {
            if (vertex == null || vertex.size() != 2 || vertex.get(0) == null || vertex.get(1) == null) {
                problems.add("each corner needs a latitude and a longitude");
                return;
            }
            BigDecimal lat = vertex.get(0);
            BigDecimal lng = vertex.get(1);
            if (lat.abs().compareTo(BigDecimal.valueOf(90)) > 0 || lng.abs().compareTo(BigDecimal.valueOf(180)) > 0) {
                problems.add("corner " + lat + ", " + lng + " is not a real position");
                return;
            }
            if (isNullIsland(lat, lng)) {
                problems.add("a corner is at 0, 0, which is what a missing position looks like");
                return;
            }
            corners.add(new double[] {lat.doubleValue(), lng.doubleValue()});
        }
        if (GeofenceUtils.polygonSelfIntersects(corners)) {
            problems.add("the polygon's edges cross each other");
        }
    }

    private static boolean isNullIsland(BigDecimal lat, BigDecimal lng) {
        return lat.signum() == 0 && lng.signum() == 0;
    }

    private static Geofence.FenceType parseFenceType(String value) {
        if (value == null || value.isBlank()) {
            return Geofence.FenceType.KEEP_IN;
        }
        try {
            return Geofence.FenceType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("fenceType must be KEEP_IN (a camp) or KEEP_OUT (a restricted zone)");
        }
    }

    /** Explicit if given; otherwise a polygon when corners were sent, else a circle. */
    private static Geofence.Shape parseShape(GeofenceRequest request) {
        if (request.getShape() == null || request.getShape().isBlank()) {
            return request.getVertices() != null && !request.getVertices().isEmpty()
                    ? Geofence.Shape.POLYGON
                    : Geofence.Shape.CIRCLE;
        }
        try {
            return Geofence.Shape.valueOf(request.getShape().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("shape must be CIRCLE or POLYGON");
        }
    }

    private GeofenceResponse toResponse(Geofence fence) {
        GeofenceResponse response = new GeofenceResponse();
        response.setGeofenceId(fence.getGeofenceId());
        response.setName(fence.getName());
        response.setDescription(fence.getDescription());
        response.setFenceType(fence.getFenceType().name());
        response.setShape(fence.getShape().name());
        response.setCenterLatitude(fence.getCenterLatitude());
        response.setCenterLongitude(fence.getCenterLongitude());
        response.setRadiusMeters(fence.getRadiusMeters());
        response.setVertices(FenceShapes.vertices(fence.getVerticesJson()));
        response.setIsActive(fence.getIsActive());
        response.setRetiredAt(fence.getRetiredAt());
        response.setCreatedAt(fence.getCreatedAt());
        response.setUpdatedAt(fence.getUpdatedAt());
        if (fence.isKeepIn()) {
            response.setAnimalCount(cowRepository.countByCampGeofenceId(fence.getGeofenceId()));
            response.setAnimalsOutside(occupancyRepository.findByGeofenceId(fence.getGeofenceId()).stream()
                    .filter(occupancy -> !occupancy.getIsInside())
                    .count());
        }
        return response;
    }
}
