package com.cowtrack.controller;

import com.cowtrack.dto.request.FinancialRequest;
import com.cowtrack.dto.request.ProductionRequest;
import com.cowtrack.service.RecordsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Recording and reading the production and financial data analytics aggregates. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecordsController extends BaseController {

    private final RecordsService recordsService;

    @PostMapping("/production")
    @PreAuthorize("hasRole('FARMER') or hasRole('CARETAKER') or hasRole('ADMIN')")
    public ResponseEntity<?> recordProduction(@Valid @RequestBody ProductionRequest request) {
        return created(recordsService.recordProduction(request));
    }

    @GetMapping("/production/cow/{cowId}")
    public ResponseEntity<?> getProductionForCow(@PathVariable Long cowId) {
        return success(recordsService.getProductionForCow(cowId));
    }

    @PostMapping("/financials")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    public ResponseEntity<?> recordFinancial(@Valid @RequestBody FinancialRequest request) {
        return created(recordsService.recordFinancial(request));
    }

    @GetMapping("/financials")
    public ResponseEntity<?> getFinancials() {
        return success(recordsService.getFinancialsForCurrentUser());
    }
}
