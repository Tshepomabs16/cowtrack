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
}