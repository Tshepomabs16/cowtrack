package com.cowtrack.controller;

import com.cowtrack.dto.request.AlertFilterRequest;
import com.cowtrack.dto.response.AlertResponse;
import com.cowtrack.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController extends BaseController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<?> getAllAlerts() {
        List<AlertResponse> alerts = alertService.getAllAlerts();
        return success(alerts);
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveAlerts() {
        List<AlertResponse> alerts = alertService.getActiveAlerts();
        return success(alerts);
    }

    @GetMapping("/cow/{cowId}")
    public ResponseEntity<?> getAlertsByCow(@PathVariable Long cowId) {
        List<AlertResponse> alerts = alertService.getAlertsByCow(cowId);
        return success(alerts);
    }

    @GetMapping("/count/active")
    public ResponseEntity<?> getActiveAlertCount() {
        long count = alertService.getActiveAlertCount();
        return success("Active alerts count", count);
    }

    @PostMapping("/filter")
    public ResponseEntity<?> filterAlerts(@RequestBody AlertFilterRequest filter) {
        List<AlertResponse> alerts = alertService.filterAlerts(filter);
        return success(alerts);
    }

    /*
     * All three resolve operations are PUT. Resolving is idempotent - applying it
     * twice leaves the same state - and the client already sent PUT to each of
     * them. Two accepted only POST, so resolving a single alert from the UI
     * returned 405 and had never worked.
     */

    @PutMapping("/{alertId}/resolve")
    @PreAuthorize("hasRole('FARMER') or hasRole('CARETAKER') or hasRole('ADMIN')")
    public ResponseEntity<?> markAlertAsResolved(@PathVariable Long alertId) {
        AlertResponse alert = alertService.markAsResolved(alertId);
        return success("Alert marked as resolved", alert);
    }

    @PutMapping("/cow/{cowId}/resolve-all")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    public ResponseEntity<?> markAllAlertsAsResolved(@PathVariable Long cowId) {
        alertService.markAllAsResolved(cowId);
        return success("All alerts marked as resolved", null);
    }

    @PostMapping("/test/geofence-breach/{cowId}")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    public ResponseEntity<?> testGeofenceBreachAlert(
            @PathVariable Long cowId,
            @RequestParam boolean isInside) {
        alertService.createGeofenceBreachAlert(cowId, isInside);
        return success("Test geofence breach alert created", null);
    }

    @PostMapping("/test/no-signal/{cowId}")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    public ResponseEntity<?> testNoSignalAlert(
            @PathVariable Long cowId,
            @RequestParam long hours) {
        alertService.createNoSignalAlert(cowId, hours);
        return success("Test no signal alert created", null);
    }

    @PutMapping("/read/all")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    public ResponseEntity<?> resolveAll() {
        int resolved = alertService.resolveAllAlerts();
        return success("Resolved " + resolved + " alerts", resolved);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return success(alertService.getAlertStats());
    }

    @DeleteMapping("/{alertId}")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteAlert(@PathVariable Long alertId) {
        alertService.deleteAlert(alertId);
        return success("Alert deleted successfully", null);
    }
}
