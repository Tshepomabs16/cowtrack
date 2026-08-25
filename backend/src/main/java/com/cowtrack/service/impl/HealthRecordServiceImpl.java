package com.cowtrack.service.impl;

import com.cowtrack.dto.request.HealthRecordRequest;
import com.cowtrack.dto.response.HealthRecordResponse;
import com.cowtrack.entity.Cow;
import com.cowtrack.entity.HealthRecord;
import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.repository.CowRepository;
import com.cowtrack.repository.HealthRecordRepository;
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

    @Override
    public HealthRecordResponse createHealthRecord(HealthRecordRequest request) {
        Cow cow = cowRepository.findById(request.getCowId())
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + request.getCowId()));

        HealthRecord healthRecord = new HealthRecord();
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
        HealthRecord healthRecord = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Health record not found with id: " + recordId));
        return toResponse(healthRecord);
    }

    @Override
    public List<HealthRecordResponse> getHealthRecordsByCow(Long cowId) {
        if (!cowRepository.existsById(cowId)) {
            throw new ResourceNotFoundException("Cow not found with id: " + cowId);
        }

        return healthRecordRepository.findByCowCowIdOrderByRecordDateDesc(cowId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public HealthRecordResponse updateHealthRecord(Long recordId, HealthRecordRequest request) {
        HealthRecord healthRecord = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Health record not found with id: " + recordId));

        // Update cow if changed
        if (!healthRecord.getCow().getCowId().equals(request.getCowId())) {
            Cow newCow = cowRepository.findById(request.getCowId())
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
        if (!healthRecordRepository.existsById(recordId)) {
            throw new ResourceNotFoundException("Health record not found with id: " + recordId);
        }
        healthRecordRepository.deleteById(recordId);
    }

    @Override
    public List<HealthRecordResponse> searchHealthRecords(String query) {
        List<HealthRecord> byDiagnosis = healthRecordRepository.findByDiagnosisContainingIgnoreCase(query);
        List<HealthRecord> byVetName = healthRecordRepository.findByVetNameContainingIgnoreCase(query);

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