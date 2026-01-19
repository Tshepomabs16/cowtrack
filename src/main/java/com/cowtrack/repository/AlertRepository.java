package com.cowtrack.repository;

import com.cowtrack.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    // Find alerts for a specific cow
    List<Alert> findByCowCowIdOrderByCreatedAtDesc(Long cowId);

    // Find unresolved alerts
    List<Alert> findByIsResolvedFalseOrderByCreatedAtDesc();

    // Find alerts by type
    List<Alert> findByAlertType(Alert.AlertType alertType);

    // Count unresolved alerts
    long countByIsResolvedFalse();

    // Find alerts within time period
    @Query("SELECT a FROM Alert a WHERE a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    List<Alert> findByTimeRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Find geofence breach alerts for a cow
    @Query("SELECT a FROM Alert a WHERE a.cow.cowId = :cowId AND a.alertType = 'GEOFENCE_BREACH' ORDER BY a.createdAt DESC")
    List<Alert> findGeofenceBreachesByCowId(@Param("cowId") Long cowId);

    // ADD THIS METHOD - Find unresolved alerts for a specific cow
    List<Alert> findByCowCowIdAndIsResolvedFalse(Long cowId);
}