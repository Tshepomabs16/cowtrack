package com.cowtrack.controller;

import com.cowtrack.dto.request.HealthRecordRequest;
import com.cowtrack.dto.response.HealthRecordResponse;
import com.cowtrack.service.HealthRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/health-records")
@RequiredArgsConstructor
public class HealthRecordController extends BaseController {

    private final HealthRecordService healthRecordService;

    @PostMapping
    public ResponseEntity<?> createHealthRecord(@Valid @RequestBody HealthRecordRequest request) {
        HealthRecordResponse record = healthRecordService.createHealthRecord(request);
        return created(record);
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<?> getHealthRecordById(@PathVariable Long recordId) {
        HealthRecordResponse record = healthRecordService.getHealthRecordById(recordId);
        return success(record);
    }

    @GetMapping("/cow/{cowId}")
    public ResponseEntity<?> getHealthRecordsByCow(@PathVariable Long cowId) {
        List<HealthRecordResponse> records = healthRecordService.getHealthRecordsByCow(cowId);
        return success(records);
    }

    @PutMapping("/{recordId}")
    public ResponseEntity<?> updateHealthRecord(
            @PathVariable Long recordId,
            @Valid @RequestBody HealthRecordRequest request) {
        HealthRecordResponse record = healthRecordService.updateHealthRecord(recordId, request);
        return success("Health record updated successfully", record);
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<?> deleteHealthRecord(@PathVariable Long recordId) {
        healthRecordService.deleteHealthRecord(recordId);
        return success("Health record deleted successfully", null);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchHealthRecords(@RequestParam String query) {
        List<HealthRecordResponse> records = healthRecordService.searchHealthRecords(query);
        return success(records);
    }
}