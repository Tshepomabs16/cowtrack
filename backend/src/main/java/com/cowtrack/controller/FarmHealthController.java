package com.cowtrack.controller;

import com.cowtrack.dto.request.HealthMetricRequest;
import com.cowtrack.dto.request.HealthRecordRequest;
import com.cowtrack.dto.request.VaccinationRequest;
import com.cowtrack.service.FarmHealthService;
import com.cowtrack.service.HealthRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Herd health endpoints under {@code /api/health}.
 *
 * <p>Note that the bare {@code /api/health} liveness probe lives on
 * {@link HealthController}; the paths here are all nested below it and so do not
 * collide with it.
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class FarmHealthController extends BaseController {

    private final FarmHealthService farmHealthService;
    private final HealthRecordService healthRecordService;

    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics(
            @RequestParam(required = false, defaultValue = "7d") String range) {
        return success(farmHealthService.getHerdMetrics(range));
    }

    @GetMapping("/cows/{cowId}")
    public ResponseEntity<?> getCowHealth(@PathVariable Long cowId) {
        return success(farmHealthService.getMetricsForCow(cowId));
    }

    @PostMapping("/metrics")
    public ResponseEntity<?> recordMetric(@Valid @RequestBody HealthMetricRequest request) {
        return created(farmHealthService.recordMetric(request));
    }

    /** A veterinary checkup, stored as a health record. */
    @PostMapping("/checkups")
    public ResponseEntity<?> recordCheckup(@Valid @RequestBody HealthRecordRequest request) {
        return created(healthRecordService.createHealthRecord(request));
    }

    @GetMapping("/vaccinations")
    public ResponseEntity<?> getVaccinations(@RequestParam(required = false) String status) {
        return success(farmHealthService.getVaccinations(status));
    }

    @PostMapping("/vaccinations")
    public ResponseEntity<?> scheduleVaccination(@Valid @RequestBody VaccinationRequest request) {
        return created(farmHealthService.scheduleVaccination(request));
    }

    @GetMapping("/reports")
    public ResponseEntity<?> getReports(
            @RequestParam(required = false, defaultValue = "month") String range) {
        return success(farmHealthService.getReports(range));
    }
}
