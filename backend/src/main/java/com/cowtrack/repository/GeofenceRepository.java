package com.cowtrack.repository;

import com.cowtrack.entity.Geofence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GeofenceRepository extends JpaRepository<Geofence, Long> {

    Optional<Geofence> findByCowCowId(Long cowId);

    @Query("SELECT g FROM Geofence g WHERE g.cow.tagId = :tagId")
    Optional<Geofence> findByCowTagId(@Param("tagId") String tagId);

    @Query("SELECT g FROM Geofence g WHERE g.cow.caretaker.userId = :caretakerId")
    List<Geofence> findByCaretakerId(@Param("caretakerId") Long caretakerId);

    @Query("SELECT g FROM Geofence g WHERE " +
            "(6371000 * 2 * ASIN(SQRT(" +
            "POWER(SIN((:latitude - g.centerLatitude) * PI() / 180 / 2), 2) + " +
            "COS(:latitude * PI() / 180) * COS(g.centerLatitude * PI() / 180) * " +
            "POWER(SIN((:longitude - g.centerLongitude) * PI() / 180 / 2), 2)" +
            "))) <= :radius")
    List<Geofence> findNearLocation(
            @Param("latitude") java.math.BigDecimal latitude,
            @Param("longitude") java.math.BigDecimal longitude,
            @Param("radius") Double radius);

    List<Geofence> findByFarmFarmId(Long farmId);

    long countByFarmFarmId(Long farmId);
}