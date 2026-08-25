package com.cowtrack.controller;

import com.cowtrack.dto.request.LocationHistoryRequest;
import com.cowtrack.dto.request.LocationRequest;
import com.cowtrack.dto.response.LocationResponse;
import com.cowtrack.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController extends BaseController {

    private final LocationService locationService;

    @PostMapping("/record")
    public ResponseEntity<?> recordLocation(@Valid @RequestBody LocationRequest request) {
        LocationResponse location = locationService.recordLocation(request);
        return created(location);
    }

    @GetMapping("/cow/{cowId}/current")
    public ResponseEntity<?> getCurrentLocation(@PathVariable Long cowId) {
        LocationResponse location = locationService.getCurrentLocation(cowId);
        return success(location);
    }

    @GetMapping("/cow/{cowId}/history")
    public ResponseEntity<?> getLocationHistory(
            @PathVariable Long cowId,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        List<LocationResponse> locations = locationService.getLocationHistory(cowId, limit);
        return success(locations);
    }

    @PostMapping("/cow/{cowId}/history-range")
    public ResponseEntity<?> getLocationsInTimeRange(
            @PathVariable Long cowId,
            @Valid @RequestBody LocationHistoryRequest request) {
        List<LocationResponse> locations = locationService.getLocationsInTimeRange(
                cowId, request.getStartDate(), request.getEndDate());
        return success(locations);
    }

    @GetMapping("/cow/{cowId}/distance")
    public ResponseEntity<?> getDistanceTraveled(
            @PathVariable Long cowId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        double distance = locationService.calculateDistanceTraveled(cowId, start, end);
        return success("Distance traveled in meters", distance);
    }

    @PostMapping("/cow/{cowId}/check-geofence")
    public ResponseEntity<?> checkGeofenceViolations(@PathVariable Long cowId) {
        locationService.checkGeofenceViolations(cowId);
        return success("Geofence check completed", null);
    }

    // SIMULATION ENDPOINT - For testing with your phone
    @PostMapping("/simulate/{cowId}")
    public ResponseEntity<?> simulateLocationUpdate(
            @PathVariable Long cowId,
            @RequestParam Double lat,
            @RequestParam Double lng) {
        LocationRequest request = new LocationRequest();
        request.setCowId(cowId);
        request.setLatitude(java.math.BigDecimal.valueOf(lat));
        request.setLongitude(java.math.BigDecimal.valueOf(lng));
        request.setAccuracy(java.math.BigDecimal.valueOf(10.0)); // 10 meter accuracy

        LocationResponse location = locationService.recordLocation(request);
        return success("Location simulated and recorded", location);
    }
}