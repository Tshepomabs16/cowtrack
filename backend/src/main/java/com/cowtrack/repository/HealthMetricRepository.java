package com.cowtrack.repository;

import com.cowtrack.entity.HealthMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HealthMetricRepository extends JpaRepository<HealthMetric, Long> {

    List<HealthMetric> findByCow_CowIdOrderByRecordedAtDesc(Long cowId);

    Optional<HealthMetric> findFirstByCow_CowIdOrderByRecordedAtDesc(Long cowId);

    List<HealthMetric> findByRecordedAtBetweenOrderByRecordedAtAsc(
            LocalDateTime start, LocalDateTime end);

    @Query("""
            SELECT CAST(m.recordedAt AS date), AVG(m.temperature),
                   AVG(m.heartRate), AVG(m.activityLevel)
            FROM HealthMetric m
            WHERE m.recordedAt BETWEEN :start AND :end
            GROUP BY CAST(m.recordedAt AS date)
            ORDER BY CAST(m.recordedAt AS date)
            """)
    List<Object[]> aggregateDailyAverages(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    List<HealthMetric> findByFarmFarmIdOrderByRecordedAtDesc(Long farmId);

    Optional<HealthMetric> findFirstByFarmFarmIdAndCowCowIdOrderByRecordedAtDesc(Long farmId, Long cowId);

    @Query("""
            SELECT CAST(m.recordedAt AS date), AVG(m.temperature),
                   AVG(m.heartRate), AVG(m.activityLevel)
            FROM HealthMetric m
            WHERE m.farm.farmId = :farmId AND m.recordedAt BETWEEN :start AND :end
            GROUP BY CAST(m.recordedAt AS date)
            ORDER BY CAST(m.recordedAt AS date)
            """)
    List<Object[]> aggregateDailyAveragesByFarm(@Param("farmId") Long farmId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);
}
