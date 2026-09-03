package com.cowtrack.repository;

import com.cowtrack.entity.Cow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * One page of a farm's herd, optionally narrowed by a search term.
     *
     * <p>Matches on tag as well as name: a farmer looking for one animal in a
     * herd of thousands is far likelier to have the ear tag to hand than to
     * remember what they called it.
     *
     * <p>The search is applied by the database rather than to the page after it
     * arrives. Filtering afterwards would search only within whichever page the
     * caller happened to be on, so whether an animal could be found would depend
     * on where it fell in the herd.
     */
    @Query("""
            SELECT c FROM Cow c
            WHERE c.farm.farmId = :farmId
              AND (:search IS NULL
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.tagId) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Cow> findPageForFarm(
            @Param("farmId") Long farmId,
            @Param("search") String search,
            Pageable pageable);
}