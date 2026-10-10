package com.cowtrack.repository;

import com.cowtrack.entity.GeofenceOccupancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeofenceOccupancyRepository
        extends JpaRepository<GeofenceOccupancy, GeofenceOccupancy.Key> {

    List<GeofenceOccupancy> findByGeofenceId(Long geofenceId);

    @Modifying
    @Query("DELETE FROM GeofenceOccupancy o WHERE o.geofenceId = :geofenceId")
    void deleteByGeofenceId(@Param("geofenceId") Long geofenceId);

    @Modifying
    @Query("DELETE FROM GeofenceOccupancy o WHERE o.geofenceId = :geofenceId AND o.cowId = :cowId")
    void deleteByGeofenceIdAndCowId(@Param("geofenceId") Long geofenceId, @Param("cowId") Long cowId);
}
