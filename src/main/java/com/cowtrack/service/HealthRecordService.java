package com.cowtrack.service;

import com.cowtrack.dto.request.HealthRecordRequest;
import com.cowtrack.dto.response.HealthRecordResponse;
import java.util.List;

public interface HealthRecordService {
    HealthRecordResponse createHealthRecord(HealthRecordRequest request);
    HealthRecordResponse getHealthRecordById(Long recordId);
    List<HealthRecordResponse> getHealthRecordsByCow(Long cowId);
    HealthRecordResponse updateHealthRecord(Long recordId, HealthRecordRequest request);
    void deleteHealthRecord(Long recordId);
    List<HealthRecordResponse> searchHealthRecords(String query);
}