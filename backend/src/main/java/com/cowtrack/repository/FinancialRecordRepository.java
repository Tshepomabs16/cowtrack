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

    /**
     * Monthly revenue and cost for one user. Returns rows of
     * {@code [year, month, totalRevenue, totalCost]}.
     *
     * <p>Uses YEAR/MONTH rather than date truncation so the query works on both
     * MySQL and the H2 instance the tests run against.
     */
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
            SELECT COALESCE(SUM(f.amount), 0) FROM FinancialRecord f
            WHERE f.user.userId = :userId AND f.entryType = :type
              AND f.recordDate BETWEEN :start AND :end
            """)
    BigDecimal totalByType(@Param("userId") Long userId,
                           @Param("type") FinancialRecord.EntryType type,
                           @Param("start") LocalDate start,
                           @Param("end") LocalDate end);
}
