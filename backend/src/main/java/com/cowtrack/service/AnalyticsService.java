package com.cowtrack.service;

import java.util.List;
import java.util.Map;

/**
 * Aggregate reporting over the herd, its production and the farm's finances.
 *
 * <p>Series are returned as plain maps because they exist purely to feed charts;
 * introducing a DTO per chart would add types without adding meaning.
 */
public interface AnalyticsService {

    /** Headline counts and KPI figures for the dashboard. */
    Map<String, Object> getDashboardStats();

    /** Daily milk and average weight over the range, plus breed distribution. */
    Map<String, Object> getProductionTrends(String range);

    /** Daily average vitals over the range. */
    Map<String, Object> getHealthTrends(String range);

    /** Monthly revenue and cost for the authenticated user. */
    Map<String, Object> getFinancials(String range);

    /**
     * Simple projections extrapolated from recent history. These are trend
     * estimates, not a trained model.
     */
    List<Map<String, Object>> getPredictions();
}
