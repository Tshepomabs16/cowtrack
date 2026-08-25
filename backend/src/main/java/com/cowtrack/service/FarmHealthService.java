package com.cowtrack.service;

import com.cowtrack.dto.request.HealthMetricRequest;
import com.cowtrack.dto.request.VaccinationRequest;
import com.cowtrack.dto.response.HealthMetricResponse;
import com.cowtrack.dto.response.VaccinationResponse;

import java.util.List;
import java.util.Map;

/**
 * Collar vitals and vaccination scheduling.
 *
 * <p>Distinct from {@link HealthRecordService}, which deals with veterinary
 * diagnosis and treatment records.
 */
public interface FarmHealthService {

    /** Latest reading per animal, plus herd-level counts. */
    Map<String, Object> getHerdMetrics(String range);

    List<HealthMetricResponse> getMetricsForCow(Long cowId);

    HealthMetricResponse recordMetric(HealthMetricRequest request);

    List<VaccinationResponse> getVaccinations(String status);

    VaccinationResponse scheduleVaccination(VaccinationRequest request);

    /** Summary counts suitable for the health reports view. */
    Map<String, Object> getReports(String range);
}
