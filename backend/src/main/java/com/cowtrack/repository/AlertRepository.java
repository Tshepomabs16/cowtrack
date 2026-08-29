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
}