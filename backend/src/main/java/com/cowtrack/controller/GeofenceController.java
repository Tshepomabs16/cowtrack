package com.cowtrack.controller;

import com.cowtrack.dto.request.GeofenceRequest;
import com.cowtrack.dto.response.GeofenceResponse;
import com.cowtrack.service.GeofenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/geofences")
@RequiredArgsConstructor
public class GeofenceController extends BaseController {

    private final GeofenceService geofenceService;

    @PostMapping
    public ResponseEntity<?> createGeofence(@Valid @RequestBody GeofenceRequest request) {
        GeofenceResponse geofence = geofenceService.createGeofence(request);
        return created(geofence);
    }

    @GetMapping("/cow/{cowId}")
    public ResponseEntity<?> getGeofenceByCowId(@PathVariable Long cowId) {
        GeofenceResponse geofence = geofenceService.getGeofenceByCowId(cowId);
        return success(geofence);
    }

    @GetMapping("/caretaker/{caretakerId}")
    public ResponseEntity<?> getGeofencesByCaretaker(@PathVariable Long caretakerId) {
        List<GeofenceResponse> geofences = geofenceService.getGeofencesByCaretaker(caretakerId);
        return success(geofences);
    }

    @PutMapping("/{geofenceId}")
    public ResponseEntity<?> updateGeofence(
            @PathVariable Long geofenceId,
            @Valid @RequestBody GeofenceRequest request) {
        GeofenceResponse geofence = geofenceService.updateGeofence(geofenceId, request);
        return success("Geofence updated successfully", geofence);
    }

    @DeleteMapping("/{geofenceId}")
    public ResponseEntity<?> deleteGeofence(@PathVariable Long geofenceId) {
        geofenceService.deleteGeofence(geofenceId);
        return success("Geofence deleted successfully", null);
    }

    @PostMapping("/{geofenceId}/activate")
    public ResponseEntity<?> activateGeofence(@PathVariable Long geofenceId) {
        GeofenceResponse geofence = geofenceService.activateGeofence(geofenceId);
        return success("Geofence activated", geofence);
    }

    @PostMapping("/{geofenceId}/deactivate")
    public ResponseEntity<?> deactivateGeofence(@PathVariable Long geofenceId) {
        GeofenceResponse geofence = geofenceService.deactivateGeofence(geofenceId);
        return success("Geofence deactivated", geofence);
    }

    @PostMapping("/check-location")
    public ResponseEntity<?> checkLocationInGeofence(
            @RequestParam Long cowId,
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude) {
        boolean isInside = geofenceService.isLocationInsideGeofence(cowId, latitude, longitude);
        return success(isInside ? "Inside geofence" : "Outside geofence", isInside);
    }
}