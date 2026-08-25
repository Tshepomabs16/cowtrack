package com.cowtrack.controller;

import com.cowtrack.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController extends BaseController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        return success(analyticsService.getDashboardStats());
    }

    @GetMapping("/production")
    public ResponseEntity<?> getProductionTrends(
            @RequestParam(required = false, defaultValue = "month") String range) {
        return success(analyticsService.getProductionTrends(range));
    }

    @GetMapping("/health")
    public ResponseEntity<?> getHealthTrends(
            @RequestParam(required = false, defaultValue = "7d") String range) {
        return success(analyticsService.getHealthTrends(range));
    }

    @GetMapping("/financials")
    public ResponseEntity<?> getFinancials(
            @RequestParam(required = false, defaultValue = "year") String range) {
        return success(analyticsService.getFinancials(range));
    }

    @GetMapping("/predictions")
    public ResponseEntity<?> getPredictions() {
        return success(analyticsService.getPredictions());
    }
}
