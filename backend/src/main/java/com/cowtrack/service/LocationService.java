package com.cowtrack.service;

import com.cowtrack.dto.request.LocationRequest;
import com.cowtrack.dto.response.LocationResponse;
import java.util.List;

public interface LocationService {
    LocationResponse recordLocation(LocationRequest request);
    List<LocationResponse> getLocationHistory(Long cowId, Integer limit);
    LocationResponse getCurrentLocation(Long cowId);
    List<LocationResponse> getLocationsInTimeRange(Long cowId, java.time.LocalDateTime start, java.time.LocalDateTime end);
    void checkGeofenceViolations(Long cowId);
    double calculateDistanceTraveled(Long cowId, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
