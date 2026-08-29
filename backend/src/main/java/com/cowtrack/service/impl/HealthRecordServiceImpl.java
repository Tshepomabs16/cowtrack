package com.cowtrack.service.impl;

import com.cowtrack.dto.request.HealthRecordRequest;
import com.cowtrack.dto.response.HealthRecordResponse;
import com.cowtrack.entity.Cow;
import com.cowtrack.entity.Farm;
import com.cowtrack.entity.HealthRecord;
import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.repository.CowRepository;
import com.cowtrack.repository.FarmRepository;
import com.cowtrack.repository.HealthRecordRepository;
import com.cowtrack.security.FarmContext;
import com.cowtrack.service.HealthRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class HealthRecordServiceImpl implements HealthRecordService {

    private final HealthRecordRepository healthRecordRepository;
    private final CowRepository cowRepository;
    private final FarmContext farmContext;
    private final FarmRepository farmRepository;

    @Override
    public HealthRecordResponse createHealthRecord(HealthRecordRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        Cow cow = cowRepository.findByFarmFarmIdAndCowId(farmId, request.getCowId())
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + request.getCowId()));

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));

        HealthRecord healthRecord = new HealthRecord();
        healthRecord.setFarm(farm);
        healthRecord.setCow(cow);
        healthRecord.setDiagnosis(request.getDiagnosis());
        healthRecord.setTreatment(request.getTreatment());
        healthRecord.setVetName(request.getVetName());
        healthRecord.setRecordDate(request.getRecordDate());
        healthRecord.setCreatedAt(LocalDateTime.now());

        HealthRecord savedRecord = healthRecordRepository.save(healthRecord);
        return toResponse(savedRecord);
    }

    @Override
    public HealthRecordResponse getHealthRecordById(Long recordId) {
        Long farmId = farmContext.getCurrentFarmId();
        HealthRecord healthRecord = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Health record not found with id: " + recordId));
        if (healthRecord.getFarm() == null || !healthRecord.getFarm().getFarmId().equals(farmId)) {
            throw new ResourceNotFoundException("Health record not found with id: " + recordId);
        }
        return toResponse(healthRecord);
    }

    @Override
    public List<HealthRecordResponse> getHealthRecordsByCow(Long cowId) {
        Long farmId = farmContext.getCurrentFarmId();
        if (!cowRepository.findByFarmFarmIdAndCowId(farmId, cowId).isPresent()) {
            throw new ResourceNotFoundException("Cow not found with id: " + cowId);
        }

        return healthRecordRepository.findByCowCowIdOrderByRecordDateDesc(cowId).stream()
                .filter(r -> r.getFarm() != null && r.getFarm().getFarmId().equals(farmId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public HealthRecordResponse updateHealthRecord(Long recordId, HealthRecordRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        HealthRecord healthRecord = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Health record not found with id: " + recordId));
        if (healthRecord.getFarm() == null || !healthRecord.getFarm().getFarmId().equals(farmId)) {
            throw new ResourceNotFoundException("Health record not found with id: " + recordId);
        }

        if (!healthRecord.getCow().getCowId().equals(request.getCowId())) {
            Cow newCow = cowRepository.findByFarmFarmIdAndCowId(farmId, request.getCowId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + request.getCowId()));
            healthRecord.setCow(newCow);
        }

        healthRecord.setDiagnosis(request.getDiagnosis());
        healthRecord.setTreatment(request.getTreatment());
        healthRecord.setVetName(request.getVetName());
        healthRecord.setRecordDate(request.getRecordDate());

        HealthRecord updatedRecord = healthRecordRepository.save(healthRecord);
        return toResponse(updatedRecord);
    }

    @Override
    public void deleteHealthRecord(Long recordId) {
        Long farmId = farmContext.getCurrentFarmId();
        HealthRecord healthRecord = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Health record not found with id: " + recordId));
        if (healthRecord.getFarm() == null || !healthRecord.getFarm().getFarmId().equals(farmId)) {
            throw new ResourceNotFoundException("Health record not found with id: " + recordId);
        }
        healthRecordRepository.deleteById(recordId);
    }

    @Override
    public List<HealthRecordResponse> searchHealthRecords(String query) {
        Long farmId = farmContext.getCurrentFarmId();
        List<HealthRecord> byDiagnosis = healthRecordRepository.findByFarmFarmIdAndDiagnosisContainingIgnoreCase(farmId, query);
        List<HealthRecord> byVetName = healthRecordRepository.findByFarmFarmIdAndVetNameContainingIgnoreCase(farmId, query);

        // Combine and remove duplicates
        return java.util.stream.Stream.concat(byDiagnosis.stream(), byVetName.stream())
                .distinct()
                .sorted((r1, r2) -> r2.getRecordDate().compareTo(r1.getRecordDate()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private HealthRecordResponse toResponse(HealthRecord healthRecord) {
        HealthRecordResponse response = new HealthRecordResponse();
        response.setHealthId(healthRecord.getHealthId());
        response.setCowId(healthRecord.getCow().getCowId());
        response.setCowName(healthRecord.getCow().getName());
        response.setDiagnosis(healthRecord.getDiagnosis());
        response.setTreatment(healthRecord.getTreatment());
        response.setVetName(healthRecord.getVetName());
        response.setRecordDate(healthRecord.getRecordDate());
        response.setCreatedAt(healthRecord.getCreatedAt());
        return response;
    }
}