package com.cowtrack.service;

import com.cowtrack.dto.request.GeofenceRequest;
import com.cowtrack.dto.response.GeofenceDeletionResult;
import com.cowtrack.dto.response.GeofenceResponse;

import java.math.BigDecimal;
import java.util.List;

/** Camps and restricted zones on the caller's farm. */
public interface GeofenceService {

    /** The farm's fences, newest first; retired ones only when asked for. */
    List<GeofenceResponse> getGeofences(boolean includeRetired);

    GeofenceResponse getGeofence(Long geofenceId);

    /** The camp an animal is in. */
    GeofenceResponse getCampForCow(Long cowId);

    GeofenceResponse createGeofence(GeofenceRequest request);

    GeofenceResponse updateGeofence(Long geofenceId, GeofenceRequest request);

    /**
     * Removes a fence, or retires it if it has ever raised an alert so that
     * those alerts keep the boundary they were raised against.
     */
    GeofenceDeletionResult deleteGeofence(Long geofenceId);

    /** Switches a fence off, e.g. a camp being rested. Refused while animals are in it. */
    GeofenceResponse deactivateGeofence(Long geofenceId);

    GeofenceResponse activateGeofence(Long geofenceId);

    /** Moves animals into a camp, taking each out of the camp it was in. */
    GeofenceResponse assignAnimals(Long campId, List<Long> cowIds);

    /** Takes one animal out of a camp without putting it anywhere else. */
    GeofenceResponse removeAnimal(Long campId, Long cowId);

    /** Whether a point is inside the animal's camp; false if it has none. */
    boolean isLocationInsideGeofence(Long cowId, BigDecimal latitude, BigDecimal longitude);
}
