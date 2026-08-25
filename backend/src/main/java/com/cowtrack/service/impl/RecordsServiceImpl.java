package com.cowtrack.service.impl;

import com.cowtrack.dto.request.FinancialRequest;
import com.cowtrack.dto.request.ProductionRequest;
import com.cowtrack.dto.response.ProductionResponse;
import com.cowtrack.entity.Cow;
import com.cowtrack.entity.FinancialRecord;
import com.cowtrack.entity.ProductionRecord;
import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.repository.CowRepository;
import com.cowtrack.repository.FinancialRecordRepository;
import com.cowtrack.repository.ProductionRecordRepository;
import com.cowtrack.service.RecordsService;
import com.cowtrack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RecordsServiceImpl implements RecordsService {

    private final ProductionRecordRepository productionRepository;
    private final FinancialRecordRepository financialRepository;
    private final CowRepository cowRepository;
    private final UserService userService;

    @Override
    public ProductionResponse recordProduction(ProductionRequest request) {
        Cow cow = cowRepository.findById(request.getCowId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cow not found with id: " + request.getCowId()));

        // One record per cow per day: recording twice updates rather than
        // duplicating, which would otherwise double that day's totals.
        ProductionRecord record = productionRepository
                .findByCow_CowIdAndRecordDate(cow.getCowId(), request.getRecordDate())
                .orElseGet(ProductionRecord::new);

        record.setCow(cow);
        record.setRecordDate(request.getRecordDate());
        record.setMilkLitres(request.getMilkLitres());
        record.setWeightKg(request.getWeightKg());

        return toResponse(productionRepository.save(record));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductionResponse> getProductionForCow(Long cowId) {
        if (!cowRepository.existsById(cowId)) {
            throw new ResourceNotFoundException("Cow not found with id: " + cowId);
        }
        return productionRepository.findByCow_CowIdOrderByRecordDateDesc(cowId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> recordFinancial(FinancialRequest request) {
        FinancialRecord record = new FinancialRecord();
        record.setUser(userService.getAuthenticatedUser());
        record.setEntryType(FinancialRecord.EntryType.valueOf(
                request.getEntryType().toUpperCase()));
        record.setAmount(request.getAmount());
        record.setCategory(request.getCategory());
        record.setDescription(request.getDescription());
        record.setRecordDate(request.getRecordDate());

        if (request.getCowId() != null) {
            record.setCow(cowRepository.findById(request.getCowId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cow not found with id: " + request.getCowId())));
        }

        return toMap(financialRepository.save(record));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFinancialsForCurrentUser() {
        Long userId = userService.getAuthenticatedUser().getUserId();
        return financialRepository.findByUser_UserIdOrderByRecordDateDesc(userId).stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    private ProductionResponse toResponse(ProductionRecord record) {
        return new ProductionResponse(
                record.getProductionId(),
                record.getCow().getCowId(),
                record.getCow().getName(),
                record.getRecordDate(),
                record.getMilkLitres(),
                record.getWeightKg()
        );
    }

    private Map<String, Object> toMap(FinancialRecord record) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("financialId", record.getFinancialId());
        entry.put("entryType", record.getEntryType().name());
        entry.put("amount", record.getAmount());
        entry.put("category", record.getCategory());
        entry.put("description", record.getDescription());
        entry.put("recordDate", record.getRecordDate());
        entry.put("cowId", record.getCow() == null ? null : record.getCow().getCowId());
        entry.put("cowName", record.getCow() == null ? null : record.getCow().getName());
        return entry;
    }
}
