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

    List<HealthRecord> findByCowCowIdOrderByRecordDateDesc(Long cowId);

    List<HealthRecord> findByCowCowIdAndRecordDateAfterOrderByRecordDateDesc(
            Long cowId, LocalDate date);

    List<HealthRecord> findByDiagnosisContainingIgnoreCase(String diagnosis);

    List<HealthRecord> findByVetNameContainingIgnoreCase(String vetName);

    @Query("SELECT MIN(hr.recordDate) as firstDate, MAX(hr.recordDate) as lastDate, COUNT(hr) as totalRecords " +
            "FROM HealthRecord hr WHERE hr.cow.cowId = :cowId")
    Object[] getHealthSummary(@Param("cowId") Long cowId);

    List<HealthRecord> findByFarmFarmIdOrderByRecordDateDesc(Long farmId);

    List<HealthRecord> findByFarmFarmIdAndDiagnosisContainingIgnoreCase(Long farmId, String diagnosis);

    long countByFarmFarmId(Long farmId);

    List<HealthRecord> findByFarmFarmIdAndVetNameContainingIgnoreCase(Long farmId, String vetName);
}