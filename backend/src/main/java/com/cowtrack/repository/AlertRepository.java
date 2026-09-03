package com.cowtrack.repository;

import com.cowtrack.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByCowCowIdOrderByCreatedAtDesc(Long cowId);

    List<Alert> findByIsResolvedFalseOrderByCreatedAtDesc();

    List<Alert> findByAlertType(Alert.AlertType alertType);

    long countByIsResolvedFalse();

    @Query("SELECT a FROM Alert a WHERE a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    List<Alert> findByTimeRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT a FROM Alert a WHERE a.cow.cowId = :cowId AND a.alertType = 'GEOFENCE_BREACH' ORDER BY a.createdAt DESC")
    List<Alert> findGeofenceBreachesByCowId(@Param("cowId") Long cowId);

    List<Alert> findByCowCowIdAndIsResolvedFalse(Long cowId);

    List<Alert> findByFarmFarmIdOrderByCreatedAtDesc(Long farmId);

    List<Alert> findByFarmFarmIdAndIsResolvedFalseOrderByCreatedAtDesc(Long farmId);

    long countByFarmFarmIdAndIsResolvedFalse(Long farmId);

    List<Alert> findByFarmFarmIdAndAlertType(Long farmId, Alert.AlertType alertType);

    List<Alert> findByFarmFarmIdAndCowCowIdOrderByCreatedAtDesc(Long farmId, Long cowId);

    List<Alert> findByFarmFarmIdAndCowCowIdAndIsResolvedFalse(Long farmId, Long cowId);

    /**
     * One page of a farm's alerts, optionally only the unresolved ones.
     *
     * <p>Alerts are resolved rather than deleted, so this table only ever grows.
     * Returning all of it is the kind of endpoint that works for a year and then
     * stops.
     *
     * <p>The cow is fetched in the same query because every response carries the
     * animal's name; without it Hibernate would resolve that eager association
     * with one extra select per alert on the page.
     */
    /**
     * The animals on a farm that currently have something unresolved against
     * them, as ids only.
     *
     * <p>One query for the whole herd. The live map colours markers by status,
     * and status depends on whether an animal has an open alert; asking that
     * question per animal is a query per head on the page a farmer opens first.
     */
    @Query("""
            SELECT DISTINCT a.cow.cowId FROM Alert a
            WHERE a.farm.farmId = :farmId AND a.isResolved = false
            """)
    List<Long> findCowIdsWithOpenAlerts(@Param("farmId") Long farmId);

    @Query("""
            SELECT a FROM Alert a
            JOIN FETCH a.cow
            WHERE a.farm.farmId = :farmId
              AND (:unresolvedOnly = false OR a.isResolved = false)
            """)
    Page<Alert> findPageForFarm(
            @Param("farmId") Long farmId,
            @Param("unresolvedOnly") boolean unresolvedOnly,
            Pageable pageable);
}