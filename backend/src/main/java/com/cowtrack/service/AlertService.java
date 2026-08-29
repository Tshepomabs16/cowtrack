package com.cowtrack.service;

import com.cowtrack.dto.request.AlertFilterRequest;
import com.cowtrack.dto.response.AlertResponse;

import java.util.List;

public interface AlertService {
    List<AlertResponse> getAllAlerts();
    List<AlertResponse> getAlertsByCow(Long cowId);
    List<AlertResponse> getActiveAlerts();
    AlertResponse markAsResolved(Long alertId);
    void markAllAsResolved(Long cowId);
    long getActiveAlertCount();
    List<AlertResponse> filterAlerts(AlertFilterRequest filter);
    void createGeofenceBreachAlert(Long cowId, boolean isInside);
    void createNoSignalAlert(Long cowId, long hoursWithoutSignal);

    /**
     * Raises a no-signal alert for an explicitly named farm.
     *
     * <p>The single-argument form resolves the farm from the security context,
     * which only exists on a request thread. The collar sweep runs on a
     * scheduler with no authenticated principal, so it has to supply the farm
     * itself.
     *
     * @return true if an alert was created, false if one was already open for
     *         this animal and the call was therefore a no-op
     */
    boolean createNoSignalAlert(Long farmId, Long cowId, long hoursWithoutSignal);

    /** Resolves every open alert across the herd. */
    int resolveAllAlerts();

    void deleteAlert(Long alertId);

    /** Counts by severity and type for the alerts dashboard. */
    java.util.Map<String, Object> getAlertStats();
}