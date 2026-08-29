package com.cowtrack.repository;

import com.cowtrack.entity.LocationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRecordRepository extends JpaRepository<LocationRecord, Long> {

    List<LocationRecord> findByCowCowIdOrderByRecordedAtDesc(Long cowId);

    // Deliberately absent: a latest-by-cow lookup with no farm predicate. It
    // returned records written by any farm, which put one farm's injected
    // position onto another farm's live map. Use findLatestByFarmIdAndCowId.

    @Query("SELECT lr FROM LocationRecord lr WHERE lr.cow.cowId = :cowId AND lr.recordedAt BETWEEN :start AND :end ORDER BY lr.recordedAt DESC")
    List<LocationRecord> findByCowIdAndTimeRange(
            @Param("cowId") Long cowId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT lr FROM LocationRecord lr WHERE lr.cow.cowId = :cowId AND " +
            "lr.latitude BETWEEN :minLat AND :maxLat AND " +
            "lr.longitude BETWEEN :minLng AND :maxLng ORDER BY lr.recordedAt DESC")
    List<LocationRecord> findByCowIdInArea(
            @Param("cowId") Long cowId,
            @Param("minLat") java.math.BigDecimal minLat,
            @Param("maxLat") java.math.BigDecimal maxLat,
            @Param("minLng") java.math.BigDecimal minLng,
            @Param("maxLng") java.math.BigDecimal maxLng);

    @Query("SELECT DATE(lr.recordedAt) as day, COUNT(lr) as count " +
            "FROM LocationRecord lr WHERE lr.cow.cowId = :cowId " +
            "GROUP BY DATE(lr.recordedAt) " +
            "ORDER BY day DESC")
    List<Object[]> countLocationsPerDay(@Param("cowId") Long cowId);

    List<LocationRecord> findByFarmFarmIdOrderByRecordedAtDesc(Long farmId);

    @Query("SELECT lr FROM LocationRecord lr WHERE lr.farm.farmId = :farmId ORDER BY lr.recordedAt DESC LIMIT 1")
    Optional<LocationRecord> findLatestByFarmId(@Param("farmId") Long farmId);

    long countByFarmFarmId(Long farmId);

    List<LocationRecord> findByFarmFarmIdAndCowCowIdOrderByRecordedAtDesc(Long farmId, Long cowId);

    @Query("SELECT lr FROM LocationRecord lr WHERE lr.farm.farmId = :farmId AND lr.cow.cowId = :cowId ORDER BY lr.recordedAt DESC LIMIT 1")
    Optional<LocationRecord> findLatestByFarmIdAndCowId(@Param("farmId") Long farmId, @Param("cowId") Long cowId);

    @Query("SELECT lr FROM LocationRecord lr WHERE lr.farm.farmId = :farmId AND lr.cow.cowId = :cowId AND lr.recordedAt BETWEEN :start AND :end ORDER BY lr.recordedAt DESC")
    List<LocationRecord> findByFarmIdAndCowIdAndTimeRange(
            @Param("farmId") Long farmId,
            @Param("cowId") Long cowId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}