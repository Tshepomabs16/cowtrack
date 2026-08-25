package com.cowtrack.service.impl;

import com.cowtrack.dto.request.AlertFilterRequest;
import com.cowtrack.dto.response.AlertResponse;
import com.cowtrack.entity.Alert;
import com.cowtrack.entity.Cow;
import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.repository.AlertRepository;
import com.cowtrack.repository.CowRepository;
import com.cowtrack.service.AlertService;
import com.cowtrack.util.AlertMessageGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final CowRepository cowRepository;
    private final AlertMessageGenerator alertMessageGenerator;

    @Override
    public List<AlertResponse> getAllAlerts() {
        return alertRepository.findAll().stream()
                .sorted((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertResponse> getAlertsByCow(Long cowId) {
        if (!cowRepository.existsById(cowId)) {
            throw new ResourceNotFoundException("Cow not found with id: " + cowId);
        }

        return alertRepository.findByCowCowIdOrderByCreatedAtDesc(cowId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertResponse> getActiveAlerts() {
        return alertRepository.findByIsResolvedFalseOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AlertResponse markAsResolved(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + alertId));

        alert.setIsResolved(true);
        Alert updatedAlert = alertRepository.save(alert);

        log.info("Alert {} marked as resolved", alertId);
        return toResponse(updatedAlert);
    }

    @Override
    public void markAllAsResolved(Long cowId) {
        // Use the method we just added
        List<Alert> activeAlerts = alertRepository.findByCowCowIdAndIsResolvedFalse(cowId);
        activeAlerts.forEach(alert -> alert.setIsResolved(true));
        alertRepository.saveAll(activeAlerts);

        log.info("Marked {} alerts as resolved for cow {}", activeAlerts.size(), cowId);
    }

    @Override
    public long getActiveAlertCount() {
        return alertRepository.countByIsResolvedFalse();
    }

    @Override
    public List<AlertResponse> filterAlerts(AlertFilterRequest filter) {
        List<Alert> allAlerts = alertRepository.findAll();

        return allAlerts.stream()
                .filter(alert -> filter.getCowId() == null || alert.getCow().getCowId().equals(filter.getCowId()))
                .filter(alert -> filter.getAlertTypes() == null || filter.getAlertTypes().isEmpty() ||
                        filter.getAlertTypes().contains(alert.getAlertType().name()))
                .filter(alert -> filter.getIsResolved() == null || alert.getIsResolved().equals(filter.getIsResolved()))
                .filter(alert -> filter.getStartDate() == null || !alert.getCreatedAt().isBefore(filter.getStartDate()))
                .filter(alert -> filter.getEndDate() == null || !alert.getCreatedAt().isAfter(filter.getEndDate()))
                .sorted((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()))
                .skip((long) filter.getPage() * filter.getSize())
                .limit(filter.getSize())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void createGeofenceBreachAlert(Long cowId, boolean isInside) {
        Cow cow = cowRepository.findById(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + cowId));

        Alert alert = new Alert();
        alert.setCow(cow);
        alert.setAlertType(Alert.AlertType.GEOFENCE_BREACH);
        alert.setMessage(alertMessageGenerator.generateGeofenceBreachMessage(cow, isInside));
        alert.setIsResolved(false);
        alert.setCreatedAt(LocalDateTime.now());

        alertRepository.save(alert);
        log.warn("Created geofence breach alert for cow {}: {}", cow.getTagId(), alert.getMessage());

        // In production, you would also:
        // 1. Send email/SMS notification
        // 2. Send push notification to mobile app
        // 3. Trigger alarm sound
    }

    @Override
    public void createNoSignalAlert(Long cowId, long hoursWithoutSignal) {
        Cow cow = cowRepository.findById(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + cowId));

        Alert alert = new Alert();
        alert.setCow(cow);
        alert.setAlertType(Alert.AlertType.NO_SIGNAL);
        alert.setMessage(alertMessageGenerator.generateNoSignalMessage(cow, hoursWithoutSignal));
        alert.setIsResolved(false);
        alert.setCreatedAt(LocalDateTime.now());

        alertRepository.save(alert);
        log.warn("Created no signal alert for cow {}: {} hours without signal", cow.getTagId(), hoursWithoutSignal);
    }

    private AlertResponse toResponse(Alert alert) {
        AlertResponse response = new AlertResponse();
        response.setAlertId(alert.getAlertId());
        response.setCowId(alert.getCow().getCowId());
        response.setCowName(alert.getCow().getName());
        response.setAlertType(alert.getAlertType().name());
        response.setMessage(alert.getMessage());
        response.setIsResolved(alert.getIsResolved());
        response.setCreatedAt(alert.getCreatedAt());
        response.setSeverity(deriveSeverity(alert.getAlertType()));
        response.setTitle(deriveTitle(alert.getAlertType()));
        // Note: Your schema doesn't have resolvedAt field
        // response.setResolvedAt(alert.getResolvedAt());
        return response;
    }

    /**
     * Maps an alert type onto the severity band the alerts page filters by. The
     * domain has no severity column, so it is derived from the type: anything that
     * implies the animal is unaccounted for is treated as critical.
     */
    /** A readable heading for each alert type. */
    private String deriveTitle(Alert.AlertType alertType) {
        if (alertType == null) {
            return "Alert";
        }
        return switch (alertType) {
            case GEOFENCE_BREACH -> "Geofence Breach";
            case NO_SIGNAL -> "GPS Signal Lost";
            case NIGHT_MOVEMENT -> "Unusual Night Movement";
            case DEVICE_REMOVED -> "Collar Removed";
        };
    }

    private String deriveSeverity(Alert.AlertType alertType) {
        if (alertType == null) {
            return "low";
        }
        return switch (alertType) {
            case GEOFENCE_BREACH, DEVICE_REMOVED -> "critical";
            case NO_SIGNAL -> "high";
            case NIGHT_MOVEMENT -> "medium";
        };
    }

    @Override
    public int resolveAllAlerts() {
        List<Alert> open = alertRepository.findByIsResolvedFalseOrderByCreatedAtDesc();
        open.forEach(alert -> alert.setIsResolved(true));
        alertRepository.saveAll(open);

        log.info("Resolved {} open alerts", open.size());
        return open.size();
    }

    @Override
    public void deleteAlert(Long alertId) {
        if (!alertRepository.existsById(alertId)) {
            throw new ResourceNotFoundException("Alert not found with id: " + alertId);
        }
        alertRepository.deleteById(alertId);
    }

    @Override
    public java.util.Map<String, Object> getAlertStats() {
        List<Alert> all = alertRepository.findAll();

        java.util.Map<String, Long> bySeverity = new java.util.LinkedHashMap<>();
        java.util.Map<String, Long> byType = new java.util.LinkedHashMap<>();
        long unresolved = 0;

        for (Alert alert : all) {
            if (!Boolean.TRUE.equals(alert.getIsResolved())) {
                unresolved++;
                bySeverity.merge(deriveSeverity(alert.getAlertType()), 1L, Long::sum);
            }
            byType.merge(alert.getAlertType().name(), 1L, Long::sum);
        }

        java.util.Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("total", all.size());
        stats.put("unresolved", unresolved);
        stats.put("resolved", all.size() - unresolved);
        stats.put("bySeverity", bySeverity);
        stats.put("byType", byType);
        return stats;
    }
}
