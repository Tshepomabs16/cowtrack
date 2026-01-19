package com.cowtrack.repository;

import com.cowtrack.entity.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

    // Find health records for a cow
    List<HealthRecord> findByCowCowIdOrderByRecordDateDesc(Long cowId);

    // Find recent health records
    List<HealthRecord> findByCowCowIdAndRecordDateAfterOrderByRecordDateDesc(
            Long cowId, LocalDate date);

    // Find by diagnosis (search)
    List<HealthRecord> findByDiagnosisContainingIgnoreCase(String diagnosis);

    // Find by vet name
    List<HealthRecord> findByVetNameContainingIgnoreCase(String vetName);

    // Get health summary for a cow
    @Query("SELECT MIN(hr.recordDate) as firstDate, MAX(hr.recordDate) as lastDate, COUNT(hr) as totalRecords " +
            "FROM HealthRecord hr WHERE hr.cow.cowId = :cowId")
    Object[] getHealthSummary(@Param("cowId") Long cowId);
}