package com.cowtrack.service;

import com.cowtrack.dto.request.FinancialRequest;
import com.cowtrack.dto.request.ProductionRequest;
import com.cowtrack.dto.response.ProductionResponse;

import java.util.List;
import java.util.Map;

/**
 * Entry points for the two record types the analytics views aggregate. Without
 * these the tables can only ever be populated by seeding the database directly.
 */
public interface RecordsService {

    ProductionResponse recordProduction(ProductionRequest request);

    List<ProductionResponse> getProductionForCow(Long cowId);

    Map<String, Object> recordFinancial(FinancialRequest request);

    List<Map<String, Object>> getFinancialsForCurrentUser();
}
