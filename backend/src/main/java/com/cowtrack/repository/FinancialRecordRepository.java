package com.cowtrack.repository;

import com.cowtrack.entity.FinancialRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {

    List<FinancialRecord> findByUser_UserIdOrderByRecordDateDesc(Long userId);

    @Query("""
            SELECT YEAR(f.recordDate), MONTH(f.recordDate),
                   SUM(CASE WHEN f.entryType = :revenue THEN f.amount ELSE 0 END),
                   SUM(CASE WHEN f.entryType = :cost THEN f.amount ELSE 0 END)
            FROM FinancialRecord f
            WHERE f.user.userId = :userId AND f.recordDate BETWEEN :start AND :end
            GROUP BY YEAR(f.recordDate), MONTH(f.recordDate)
            ORDER BY YEAR(f.recordDate), MONTH(f.recordDate)
            """)
    List<Object[]> aggregateMonthly(@Param("userId") Long userId,
                                    @Param("start") LocalDate start,
                                    @Param("end") LocalDate end,
                                    @Param("revenue") FinancialRecord.EntryType revenue,
                                    @Param("cost") FinancialRecord.EntryType cost);

    @Query("""
            SELECT COALESCE(f.category, 'Uncategorised'), SUM(f.amount)
            FROM FinancialRecord f
            WHERE f.user.userId = :userId AND f.entryType = :type
              AND f.recordDate BETWEEN :start AND :end
            GROUP BY f.category
            ORDER BY SUM(f.amount) DESC
            """)
    List<Object[]> aggregateByCategory(@Param("userId") Long userId,
                                       @Param("type") FinancialRecord.EntryType type,
                                       @Param("start") LocalDate start,
                                       @Param("end") LocalDate end);

    @Query("""
            SELECT COALESCE(SUM(f.amount), 0) FROM FinancialRecord f
            WHERE f.user.userId = :userId AND f.entryType = :type
              AND f.recordDate BETWEEN :start AND :end
            """)
    BigDecimal totalByType(@Param("userId") Long userId,
                           @Param("type") FinancialRecord.EntryType type,
                           @Param("start") LocalDate start,
                           @Param("end") LocalDate end);

    List<FinancialRecord> findByFarmFarmIdOrderByRecordDateDesc(Long farmId);

    @Query("""
            SELECT YEAR(f.recordDate), MONTH(f.recordDate),
                   SUM(CASE WHEN f.entryType = :revenue THEN f.amount ELSE 0 END),
                   SUM(CASE WHEN f.entryType = :cost THEN f.amount ELSE 0 END)
            FROM FinancialRecord f
            WHERE f.farm.farmId = :farmId AND f.recordDate BETWEEN :start AND :end
            GROUP BY YEAR(f.recordDate), MONTH(f.recordDate)
            ORDER BY YEAR(f.recordDate), MONTH(f.recordDate)
            """)
    List<Object[]> aggregateMonthlyByFarm(@Param("farmId") Long farmId,
                                          @Param("start") LocalDate start,
                                          @Param("end") LocalDate end,
                                          @Param("revenue") FinancialRecord.EntryType revenue,
                                          @Param("cost") FinancialRecord.EntryType cost);

    @Query("""
            SELECT COALESCE(SUM(f.amount), 0) FROM FinancialRecord f
            WHERE f.farm.farmId = :farmId AND f.entryType = :type
              AND f.recordDate BETWEEN :start AND :end
            """)
    BigDecimal totalByTypeAndFarm(@Param("farmId") Long farmId,
                                   @Param("type") FinancialRecord.EntryType type,
                                   @Param("start") LocalDate start,
                                   @Param("end") LocalDate end);

    @Query("""
            SELECT COALESCE(f.category, 'Uncategorised'), SUM(f.amount)
            FROM FinancialRecord f
            WHERE f.farm.farmId = :farmId AND f.entryType = :type
              AND f.recordDate BETWEEN :start AND :end
            GROUP BY f.category
            ORDER BY SUM(f.amount) DESC
            """)
    List<Object[]> aggregateByCategoryByFarm(@Param("farmId") Long farmId,
                                              @Param("type") FinancialRecord.EntryType type,
                                              @Param("start") LocalDate start,
                                              @Param("end") LocalDate end);
}
