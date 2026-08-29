package com.cowtrack.repository;

import com.cowtrack.entity.Cow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CowRepository extends JpaRepository<Cow, Long> {

    Optional<Cow> findByTagId(String tagId);

    List<Cow> findByCaretakerUserId(Long caretakerId);

    @Query("SELECT c FROM Cow c WHERE c NOT IN (SELECT g.cow FROM Geofence g)")
    List<Cow> findCowsWithoutGeofence();

    @Query("SELECT c FROM Cow c WHERE c.mother.cowId = :motherId OR c.father.cowId = :fatherId")
    List<Cow> findChildrenByParentId(@Param("motherId") Long motherId, @Param("fatherId") Long fatherId);

    List<Cow> findByNameContainingIgnoreCase(String name);

    boolean existsByTagId(String tagId);

    List<Cow> findByMotherCowId(Long motherId);

    List<Cow> findByFatherCowId(Long fatherId);

    List<Cow> findByFarmFarmId(Long farmId);

    List<Cow> findByFarmFarmIdAndNameContainingIgnoreCase(Long farmId, String name);

    Optional<Cow> findByFarmFarmIdAndTagId(Long farmId, String tagId);

    Optional<Cow> findByFarmFarmIdAndCowId(Long farmId, Long cowId);

    long countByFarmFarmId(Long farmId);
}