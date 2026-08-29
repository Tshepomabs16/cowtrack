package com.cowtrack.service.impl;

import com.cowtrack.dto.request.HealthMetricRequest;
import com.cowtrack.dto.request.VaccinationRequest;
import com.cowtrack.dto.response.HealthMetricResponse;
import com.cowtrack.dto.response.VaccinationResponse;
import com.cowtrack.entity.Cow;
import com.cowtrack.entity.Farm;
import com.cowtrack.entity.HealthMetric;
import com.cowtrack.entity.Vaccination;
import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.repository.CowRepository;
import com.cowtrack.repository.FarmRepository;
import com.cowtrack.repository.HealthMetricRepository;
import com.cowtrack.repository.HealthRecordRepository;
import com.cowtrack.repository.VaccinationRepository;
import com.cowtrack.security.FarmContext;
import com.cowtrack.service.FarmHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FarmHealthServiceImpl implements FarmHealthService {

    private final HealthMetricRepository healthMetricRepository;
    private final VaccinationRepository vaccinationRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final CowRepository cowRepository;
    private final FarmContext farmContext;
    private final FarmRepository farmRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getHerdMetrics(String range) {
        Long farmId = farmContext.getCurrentFarmId();
        List<HealthMetricResponse> latestPerCow = cowRepository.findByFarmFarmId(farmId).stream()
                .map(cow -> healthMetricRepository
                        .findFirstByFarmFarmIdAndCowCowIdOrderByRecordedAtDesc(farmId, cow.getCowId())
                        .orElse(null))
                .filter(Objects::nonNull)
                .map(this::toResponse)
                .collect(Collectors.toList());

        long warnings = latestPerCow.stream()
                .filter(metric -> "Warning".equals(metric.getStatus()))
                .count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("range", range);
        result.put("cows", latestPerCow);
        result.put("monitored", latestPerCow.size());
        result.put("warnings", warnings);
        result.put("normal", latestPerCow.size() - warnings);
        // Published so the client can label a reading without hardcoding the same
        // numbers a second time and letting the two drift apart.
        result.put("thresholds", Map.of(
                "temperature", HealthMetric.TEMPERATURE_WARNING_THRESHOLD,
                "heartRate", HealthMetric.HEART_RATE_WARNING_THRESHOLD,
                "activityLevel", HealthMetric.ACTIVITY_WARNING_THRESHOLD));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HealthMetricResponse> getMetricsForCow(Long cowId) {
        requireCow(cowId);
        return healthMetricRepository.findByCow_CowIdOrderByRecordedAtDesc(cowId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public HealthMetricResponse recordMetric(HealthMetricRequest request) {
        Farm farm = farmRepository.findById(farmContext.getCurrentFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        HealthMetric metric = new HealthMetric();
        metric.setFarm(farm);
        metric.setCow(requireCow(request.getCowId()));
        metric.setTemperature(request.getTemperature());
        metric.setHeartRate(request.getHeartRate());
        metric.setActivityLevel(request.getActivityLevel());
        metric.setRecordedAt(request.getRecordedAt() == null
                ? LocalDateTime.now()
                : request.getRecordedAt());

        return toResponse(healthMetricRepository.save(metric));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VaccinationResponse> getVaccinations(String status) {
        Long farmId = farmContext.getCurrentFarmId();
        List<Vaccination> vaccinations = vaccinationRepository.findByFarmFarmIdOrderByAdministeredDateDesc(farmId);

        if (status != null && !status.isBlank()) {
            Vaccination.Status wanted = Vaccination.Status.valueOf(status.toUpperCase());
            vaccinations = vaccinations.stream()
                    .filter(vaccination -> vaccination.getStatus() == wanted)
                    .collect(Collectors.toList());
        }

        return vaccinations.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public VaccinationResponse scheduleVaccination(VaccinationRequest request) {
        Farm farm = farmRepository.findById(farmContext.getCurrentFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        Vaccination vaccination = new Vaccination();
        vaccination.setFarm(farm);
        vaccination.setCow(requireCow(request.getCowId()));
        vaccination.setVaccineName(request.getVaccineName());
        vaccination.setAdministeredDate(request.getAdministeredDate());
        vaccination.setNextDueDate(request.getNextDueDate());
        vaccination.setVetName(request.getVetName());
        vaccination.setNotes(request.getNotes());

        return toResponse(vaccinationRepository.save(vaccination));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getReports(String range) {
        Long farmId = farmContext.getCurrentFarmId();
        LocalDate today = LocalDate.now();

        List<Vaccination> overdue = vaccinationRepository
                .findByFarmFarmIdAndNextDueDateBeforeOrderByNextDueDateAsc(farmId, today);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("range", range);
        report.put("totalHealthRecords", healthRecordRepository.countByFarmFarmId(farmId));
        report.put("totalVaccinations", vaccinationRepository.findByFarmFarmIdOrderByAdministeredDateDesc(farmId).size());
        report.put("overdueVaccinations",
                overdue.stream().map(this::toResponse).collect(Collectors.toList()));
        report.put("upcomingVaccinations",
                vaccinationRepository.findByFarmFarmIdAndAdministeredDateIsNullOrderByNextDueDateAsc(farmId)
                        .stream().map(this::toResponse).collect(Collectors.toList()));
        return report;
    }

    private Cow requireCow(Long cowId) {
        Long farmId = farmContext.getCurrentFarmId();
        return cowRepository.findByFarmFarmIdAndCowId(farmId, cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + cowId));
    }

    private HealthMetricResponse toResponse(HealthMetric metric) {
        return new HealthMetricResponse(
                metric.getMetricId(),
                metric.getCow().getCowId(),
                metric.getCow().getName(),
                metric.getTemperature(),
                metric.getHeartRate(),
                metric.getActivityLevel(),
                metric.getRecordedAt(),
                metric.isWarning() ? "Warning" : "Normal"
        );
    }

    private VaccinationResponse toResponse(Vaccination vaccination) {
        return new VaccinationResponse(
                vaccination.getVaccinationId(),
                vaccination.getCow().getCowId(),
                vaccination.getCow().getName(),
                vaccination.getVaccineName(),
                vaccination.getAdministeredDate(),
                vaccination.getNextDueDate(),
                vaccination.getVetName(),
                vaccination.getNotes(),
                vaccination.getStatus().name()
        );
    }
}
