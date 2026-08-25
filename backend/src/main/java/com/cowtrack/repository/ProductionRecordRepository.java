package com.cowtrack.repository;

import com.cowtrack.entity.ProductionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductionRecordRepository extends JpaRepository<ProductionRecord, Long> {

    List<ProductionRecord> findByCow_CowIdOrderByRecordDateDesc(Long cowId);

    Optional<ProductionRecord> findByCow_CowIdAndRecordDate(Long cowId, LocalDate recordDate);

    List<ProductionRecord> findByRecordDateBetweenOrderByRecordDateAsc(LocalDate start, LocalDate end);

    /**
     * Daily totals across the herd. Returns rows of
     * {@code [recordDate, totalMilkLitres, averageWeightKg]}.
     */
    @Query("""
            SELECT p.recordDate, SUM(p.milkLitres), AVG(p.weightKg)
            FROM ProductionRecord p
            WHERE p.recordDate BETWEEN :start AND :end
            GROUP BY p.recordDate
            ORDER BY p.recordDate
            """)
    List<Object[]> aggregateDailyTotals(@Param("start") LocalDate start,
                                        @Param("end") LocalDate end);

    @Query("""
            SELECT AVG(p.milkLitres) FROM ProductionRecord p
            WHERE p.recordDate BETWEEN :start AND :end
            """)
    Double averageDailyMilk(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
