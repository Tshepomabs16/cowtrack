package com.cowtrack.controller;

import com.cowtrack.dto.request.CampAssignmentRequest;
import com.cowtrack.dto.request.GeofenceRequest;
import com.cowtrack.dto.response.GeofenceDeletionResult;
import com.cowtrack.dto.response.GeofenceResponse;
import com.cowtrack.service.GeofenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Camps and restricted zones. Anyone on the farm can see them; drawing,
 * changing and moving animals between them is the farmer's decision.
 */
@RestController
@RequestMapping("/api/geofences")
@RequiredArgsConstructor
public class GeofenceController extends BaseController {

    private static final String MANAGE = "hasRole('FARMER') or hasRole('ADMIN')";

    private final GeofenceService geofenceService;

    @GetMapping
    public ResponseEntity<?> getGeofences(@RequestParam(defaultValue = "false") boolean includeRetired) {
        return success(geofenceService.getGeofences(includeRetired));
    }

    @GetMapping("/{geofenceId}")
    public ResponseEntity<?> getGeofence(@PathVariable Long geofenceId) {
        return success(geofenceService.getGeofence(geofenceId));
    }

    @GetMapping("/cow/{cowId}")
    public ResponseEntity<?> getCampForCow(@PathVariable Long cowId) {
        return success(geofenceService.getCampForCow(cowId));
    }

    @PostMapping
    @PreAuthorize(MANAGE)
    public ResponseEntity<?> createGeofence(@Valid @RequestBody GeofenceRequest request) {
        GeofenceResponse geofence = geofenceService.createGeofence(request);
        return created(geofence);
    }

    @PutMapping("/{geofenceId}")
    @PreAuthorize(MANAGE)
    public ResponseEntity<?> updateGeofence(
            @PathVariable Long geofenceId,
            @Valid @RequestBody GeofenceRequest request) {
        GeofenceResponse geofence = geofenceService.updateGeofence(geofenceId, request);
        return success("Geofence updated successfully", geofence);
    }

    @DeleteMapping("/{geofenceId}")
    @PreAuthorize(MANAGE)
    public ResponseEntity<?> deleteGeofence(@PathVariable Long geofenceId) {
        GeofenceDeletionResult result = geofenceService.deleteGeofence(geofenceId);
        return success(result.isDeleted() ? "Geofence deleted" : "Geofence retired", result);
    }

    @PostMapping("/{geofenceId}/activate")
    @PreAuthorize(MANAGE)
    public ResponseEntity<?> activateGeofence(@PathVariable Long geofenceId) {
        return success("Geofence activated", geofenceService.activateGeofence(geofenceId));
    }

    @PostMapping("/{geofenceId}/deactivate")
    @PreAuthorize(MANAGE)
    public ResponseEntity<?> deactivateGeofence(@PathVariable Long geofenceId) {
        return success("Geofence deactivated", geofenceService.deactivateGeofence(geofenceId));
    }

    @PostMapping("/{geofenceId}/animals")
    @PreAuthorize(MANAGE)
    public ResponseEntity<?> assignAnimals(
            @PathVariable Long geofenceId,
            @Valid @RequestBody CampAssignmentRequest request) {
        return success("Animals moved", geofenceService.assignAnimals(geofenceId, request.getCowIds()));
    }

    @DeleteMapping("/{geofenceId}/animals/{cowId}")
    @PreAuthorize(MANAGE)
    public ResponseEntity<?> removeAnimal(@PathVariable Long geofenceId, @PathVariable Long cowId) {
        return success("Animal taken out of camp", geofenceService.removeAnimal(geofenceId, cowId));
    }

    @PostMapping("/check-location")
    public ResponseEntity<?> checkLocationInGeofence(
            @RequestParam Long cowId,
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude) {
        boolean isInside = geofenceService.isLocationInsideGeofence(cowId, latitude, longitude);
        return success(isInside ? "Inside camp" : "Outside camp", isInside);
    }
}
