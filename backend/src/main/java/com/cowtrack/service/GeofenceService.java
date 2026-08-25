package com.cowtrack.service;

import com.cowtrack.dto.request.GeofenceRequest;
import com.cowtrack.dto.response.GeofenceResponse;

import java.util.List;

public interface GeofenceService {
    GeofenceResponse createGeofence(GeofenceRequest request);
    GeofenceResponse getGeofenceByCowId(Long cowId);
    GeofenceResponse updateGeofence(Long geofenceId, GeofenceRequest request);
    void deleteGeofence(Long geofenceId);
    List<GeofenceResponse> getGeofencesByCaretaker(Long caretakerId);
    boolean isLocationInsideGeofence(Long cowId, java.math.BigDecimal latitude, java.math.BigDecimal longitude);
    GeofenceResponse deactivateGeofence(Long geofenceId);
    GeofenceResponse activateGeofence(Long geofenceId);
}