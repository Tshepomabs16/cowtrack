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

    /** Every lookup by id goes through the farm, so one farm can never reach another's fences. */
    Optional<Geofence> findByFarmFarmIdAndGeofenceId(Long farmId, Long geofenceId);

    List<Geofence> findByFarmFarmId(Long farmId);

    /** Live fences of one type: the restricted zones that apply to a whole herd. */
    @Query("""
            SELECT g FROM Geofence g
            WHERE g.farm.farmId = :farmId AND g.fenceType = :fenceType
              AND g.isActive = true AND g.retiredAt IS NULL
            """)
    List<Geofence> findLiveByFarmAndType(
            @Param("farmId") Long farmId,
            @Param("fenceType") Geofence.FenceType fenceType);

    long countByFarmFarmId(Long farmId);
}
