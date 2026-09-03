package com.cowtrack.repository;

import com.cowtrack.entity.LocationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Every animal's newest position, for the live map, in one query.
     *
     * <p>Replaces a loop that ran a latest-position lookup per animal: fine for a
     * demo herd, one query per head on a real one. The map is the page a farmer
     * opens first, so it was also the slowest thing they touched.
     *
     * <p>Two records for the same animal can share the newest timestamp, in which
     * case both rows come back — the caller keeps the first. Ordering by id as
     * well makes which one that is deterministic rather than whatever the planner
     * returned.
     *
     * <p>The {@code JOIN FETCH} is load-bearing, not decoration. {@code cow} is a
     * {@code ManyToOne}, which defaults to eager, and JPQL does not fold an eager
     * association into the query: Hibernate runs the select below and then one
     * more per distinct animal to populate it. Since the response carries the
     * cow's name, dropping the fetch reinstates exactly the per-animal query count
     * this method exists to remove, somewhere much harder to see. Measured at 31
     * queries for 30 head without it, 1 with it.
     */
    @Query("""
            SELECT lr FROM LocationRecord lr
            JOIN FETCH lr.cow
            WHERE lr.farm.farmId = :farmId
              AND lr.recordedAt = (
                  SELECT MAX(latest.recordedAt) FROM LocationRecord latest
                  WHERE latest.cow.cowId = lr.cow.cowId
                    AND latest.farm.farmId = :farmId)
            ORDER BY lr.cow.cowId ASC, lr.locationId DESC
            """)
    List<LocationRecord> findLatestPerCowForFarm(@Param("farmId") Long farmId);

    /**
     * The most recent positions for one animal, newest first, bounded by the
     * caller.
     *
     * <p>The bound is applied by the database. Callers used to load the animal's
     * entire history and discard all but the first few rows in Java, which grows
     * without limit as a collar reports.
     */
    @Query("SELECT lr FROM LocationRecord lr WHERE lr.farm.farmId = :farmId AND lr.cow.cowId = :cowId ORDER BY lr.recordedAt DESC")
    List<LocationRecord> findRecentByFarmIdAndCowId(
            @Param("farmId") Long farmId,
            @Param("cowId") Long cowId,
            Pageable pageable);

    /** One page of an animal's history, with a total, for the history view. */
    Page<LocationRecord> findByFarmFarmIdAndCowCowId(Long farmId, Long cowId, Pageable pageable);
}