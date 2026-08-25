package com.cowtrack.service.impl;

import com.cowtrack.entity.Cow;
import com.cowtrack.entity.FinancialRecord;
import com.cowtrack.entity.User;
import com.cowtrack.repository.*;
import com.cowtrack.service.AnalyticsService;
import com.cowtrack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final CowRepository cowRepository;
    private final AlertRepository alertRepository;
    private final ProductionRecordRepository productionRepository;
    private final FinancialRecordRepository financialRepository;
    private final HealthMetricRepository healthMetricRepository;
    private final UserService userService;

    @Override
    public Map<String, Object> getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDate monthAgo = today.minusDays(30);
        User user = userService.getAuthenticatedUser();

        long totalCows = cowRepository.count();
        Double avgMilk = productionRepository.averageDailyMilk(monthAgo, today);

        BigDecimal revenue = financialRepository.totalByType(
                user.getUserId(), FinancialRecord.EntryType.REVENUE, monthAgo, today);
        BigDecimal cost = financialRepository.totalByType(
                user.getUserId(), FinancialRecord.EntryType.COST, monthAgo, today);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCows", totalCows);
        stats.put("activeAlerts", alertRepository.countByIsResolvedFalse());
        stats.put("averageMilkPerDay", round(avgMilk));
        stats.put("totalRevenue", revenue);
        stats.put("totalCost", cost);
        stats.put("netProfit", revenue.subtract(cost));
        stats.put("breedDistribution", breedDistribution());
        return stats;
    }

    @Override
    public Map<String, Object> getProductionTrends(String range) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(daysFor(range));

        List<Map<String, Object>> series = productionRepository
                .aggregateDailyTotals(start, end).stream()
                .map(row -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    LocalDate date = (LocalDate) row[0];
                    point.put("date", date.toString());
                    point.put("day", date.getDayOfWeek()
                            .getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
                    point.put("milk", round(row[1]));
                    point.put("weight", round(row[2]));
                    return point;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("range", range);
        result.put("series", series);
        result.put("breedDistribution", breedDistribution());
        return result;
    }

    @Override
    public Map<String, Object> getHealthTrends(String range) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(daysFor(range));

        List<Map<String, Object>> series = healthMetricRepository
                .aggregateDailyAverages(start, end).stream()
                .map(row -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("date", String.valueOf(row[0]));
                    point.put("temp", round(row[1]));
                    point.put("heart", round(row[2]));
                    point.put("activity", round(row[3]));
                    return point;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("range", range);
        result.put("series", series);
        return result;
    }

    @Override
    public Map<String, Object> getFinancials(String range) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(daysFor(range));
        User user = userService.getAuthenticatedUser();

        List<Map<String, Object>> series = financialRepository
                .aggregateMonthly(user.getUserId(), start, end,
                        FinancialRecord.EntryType.REVENUE, FinancialRecord.EntryType.COST)
                .stream()
                .map(row -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    int monthValue = ((Number) row[1]).intValue();
                    point.put("year", ((Number) row[0]).intValue());
                    point.put("month", Month.of(monthValue)
                            .getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
                    point.put("revenue", row[2]);
                    point.put("cost", row[3]);
                    return point;
                })
                .collect(Collectors.toList());

        BigDecimal revenue = financialRepository.totalByType(
                user.getUserId(), FinancialRecord.EntryType.REVENUE, start, end);
        BigDecimal cost = financialRepository.totalByType(
                user.getUserId(), FinancialRecord.EntryType.COST, start, end);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("range", range);
        result.put("series", series);
        result.put("totalRevenue", revenue);
        result.put("totalCost", cost);
        result.put("netProfit", revenue.subtract(cost));
        return result;
    }

    @Override
    public List<Map<String, Object>> getPredictions() {
        LocalDate today = LocalDate.now();

        // Compare the last 30 days with the 30 before it and extrapolate the delta.
        Double recent = productionRepository.averageDailyMilk(today.minusDays(30), today);
        Double previous = productionRepository.averageDailyMilk(
                today.minusDays(60), today.minusDays(31));

        List<Map<String, Object>> predictions = new ArrayList<>();
        predictions.add(prediction(
                "Milk production",
                recent == null ? null : round(recent),
                projectNext(recent, previous),
                describeTrend(recent, previous)));

        long activeAlerts = alertRepository.countByIsResolvedFalse();
        predictions.add(prediction(
                "Open alerts",
                activeAlerts,
                activeAlerts,
                activeAlerts == 0 ? "No open alerts" : "Requires attention"));

        return predictions;
    }

    /** Herd counts per breed, largest first, for the distribution pie chart. */
    private List<Map<String, Object>> breedDistribution() {
        Map<String, Long> counts = cowRepository.findAll().stream()
                .map(Cow::getBreed)
                .map(breed -> breed == null || breed.isBlank() ? "Unspecified" : breed)
                .collect(Collectors.groupingBy(breed -> breed, Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> {
                    Map<String, Object> slice = new LinkedHashMap<>();
                    slice.put("name", entry.getKey());
                    slice.put("value", entry.getValue());
                    return slice;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> prediction(String metric, Object current,
                                           Object projected, String trend) {
        Map<String, Object> prediction = new LinkedHashMap<>();
        prediction.put("metric", metric);
        prediction.put("current", current);
        prediction.put("projected", projected);
        prediction.put("trend", trend);
        return prediction;
    }

    private Object projectNext(Double recent, Double previous) {
        if (recent == null) {
            return null;
        }
        if (previous == null || previous == 0d) {
            return round(recent);
        }
        return round(recent + (recent - previous));
    }

    private String describeTrend(Double recent, Double previous) {
        if (recent == null || previous == null) {
            return "Not enough history";
        }
        if (recent > previous) {
            return "Rising";
        }
        return recent < previous ? "Falling" : "Steady";
    }

    /** Window length in days for the range keys the client sends. */
    private long daysFor(String range) {
        if (range == null) {
            return 30;
        }
        return switch (range.toLowerCase()) {
            case "7d", "week" -> 7;
            case "90d", "quarter" -> 90;
            case "year", "12m" -> 365;
            default -> 30;
        };
    }

    private BigDecimal round(Object value) {
        if (value == null) {
            return null;
        }
        return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }
}
