package com.cowtrack.service;

import com.cowtrack.dto.request.AlertFilterRequest;
import com.cowtrack.dto.response.AlertResponse;

import java.time.LocalDateTime;
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

    /**
     * Raises a night-movement alert for an animal that covered {@code metres}
     * during the night window.
     *
     * <p>Suppressed if one is already open for this animal from the current
     * night: the check runs on every incoming position, so a collar reporting
     * every few minutes would otherwise raise dozens of alerts for one event.
     *
     * @param nightStartedAt beginning of the current night window, used to decide
     *                       whether an existing open alert belongs to tonight or
     *                       to an earlier night nobody has cleared
     * @return true if an alert was created
     */
    /**
     * Raises a low-battery alert for the collar fitted to an animal. Suppressed
     * while one is already open, since the charge only goes one way.
     */
    boolean createLowBatteryAlert(Long cowId, String serialNumber, Integer batteryPercent);

    boolean createNightMovementAlert(Long cowId, double metres, LocalDateTime nightStartedAt);

    /** Resolves every open alert across the herd. */
    int resolveAllAlerts();

    void deleteAlert(Long alertId);

    /** Counts by severity and type for the alerts dashboard. */
    java.util.Map<String, Object> getAlertStats();
}