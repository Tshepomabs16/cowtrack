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

    // Find cow by tag ID
    Optional<Cow> findByTagId(String tagId);

    // Find cows by caretaker
    List<Cow> findByCaretakerUserId(Long caretakerId);

    // Find cows with no geofence (using JPQL)
    @Query("SELECT c FROM Cow c WHERE c NOT IN (SELECT g.cow FROM Geofence g)")
    List<Cow> findCowsWithoutGeofence();

    // Find child cows by parent
    @Query("SELECT c FROM Cow c WHERE c.mother.cowId = :motherId OR c.father.cowId = :fatherId")
    List<Cow> findChildrenByParentId(@Param("motherId") Long motherId, @Param("fatherId") Long fatherId);

    // Search cows by name (case-insensitive)
    List<Cow> findByNameContainingIgnoreCase(String name);

    // Check if tag ID exists
    boolean existsByTagId(String tagId);

    // ADD THESE METHODS:

    // Find cows by mother ID
    List<Cow> findByMotherCowId(Long motherId);

    // Find cows by father ID
    List<Cow> findByFatherCowId(Long fatherId);
}