package com.cowtrack.repository;

import com.cowtrack.entity.Vaccination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface VaccinationRepository extends JpaRepository<Vaccination, Long> {

    List<Vaccination> findByCow_CowIdOrderByAdministeredDateDesc(Long cowId);

    List<Vaccination> findByAdministeredDateIsNullOrderByNextDueDateAsc();

    List<Vaccination> findByNextDueDateBeforeOrderByNextDueDateAsc(LocalDate date);

    List<Vaccination> findAllByOrderByNextDueDateAsc();

    List<Vaccination> findByFarmFarmIdOrderByAdministeredDateDesc(Long farmId);

    List<Vaccination> findByFarmFarmIdAndAdministeredDateIsNullOrderByNextDueDateAsc(Long farmId);

    List<Vaccination> findByFarmFarmIdAndNextDueDateBeforeOrderByNextDueDateAsc(Long farmId, LocalDate date);
}
