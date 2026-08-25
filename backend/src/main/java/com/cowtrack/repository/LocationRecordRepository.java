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

    // Find all locations for a cow, newest first
    List<LocationRecord> findByCowCowIdOrderByRecordedAtDesc(Long cowId);

    // Find latest location for a cow
    @Query("SELECT lr FROM LocationRecord lr WHERE lr.cow.cowId = :cowId ORDER BY lr.recordedAt DESC LIMIT 1")
    Optional<LocationRecord> findLatestByCowId(@Param("cowId") Long cowId);

    // Find locations within time range
    @Query("SELECT lr FROM LocationRecord lr WHERE lr.cow.cowId = :cowId AND lr.recordedAt BETWEEN :start AND :end ORDER BY lr.recordedAt DESC")
    List<LocationRecord> findByCowIdAndTimeRange(
            @Param("cowId") Long cowId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Find locations in a geographical area
    @Query("SELECT lr FROM LocationRecord lr WHERE lr.cow.cowId = :cowId AND " +
            "lr.latitude BETWEEN :minLat AND :maxLat AND " +
            "lr.longitude BETWEEN :minLng AND :maxLng ORDER BY lr.recordedAt DESC")
    List<LocationRecord> findByCowIdInArea(
            @Param("cowId") Long cowId,
            @Param("minLat") java.math.BigDecimal minLat,
            @Param("maxLat") java.math.BigDecimal maxLat,
            @Param("minLng") java.math.BigDecimal minLng,
            @Param("maxLng") java.math.BigDecimal maxLng);

    // Count locations per day for a cow
    @Query("SELECT DATE(lr.recordedAt) as day, COUNT(lr) as count " +
            "FROM LocationRecord lr WHERE lr.cow.cowId = :cowId " +
            "GROUP BY DATE(lr.recordedAt) " +
            "ORDER BY day DESC")
    List<Object[]> countLocationsPerDay(@Param("cowId") Long cowId);
}