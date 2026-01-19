package com.cowtrack.controller;

import com.cowtrack.service.AlertService;
import com.cowtrack.service.CowService;
import com.cowtrack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController extends BaseController {

    private final UserService userService;
    private final CowService cowService;
    private final AlertService alertService;

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // These would be actual counts from repositories
        // For now, return placeholder values
        stats.put("totalUsers", 0);
        stats.put("totalCows", 0);
        stats.put("activeAlerts", alertService.getActiveAlertCount());
        stats.put("cowsWithGeofence", 0);
        stats.put("cowsWithoutSignal", 0);
        stats.put("healthRecordsToday", 0);
        stats.put("dueReminders", 0);

        return success(stats);
    }

    @GetMapping("/health-summary")
    public ResponseEntity<?> getHealthSummary() {
        Map<String, Object> summary = new HashMap<>();

        // Placeholder health summary
        summary.put("healthyCows", 0);
        summary.put("sickCows", 0);
        summary.put("recentTreatments", 0);
        summary.put("vaccinationsDue", 0);

        return success(summary);
    }

    @GetMapping("/location-summary")
    public ResponseEntity<?> getLocationSummary() {
        Map<String, Object> summary = new HashMap<>();

        // Placeholder location summary
        summary.put("cowsInsideGeofence", 0);
        summary.put("cowsOutsideGeofence", 0);
        summary.put("recentMovements", 0);
        summary.put("averageDistanceTraveled", 0);

        return success(summary);
    }
}