package com.cowtrack.repository;

import com.cowtrack.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByCowCowIdOrderByStartDateDesc(Long cowId);

    List<Reminder> findByIsCompletedFalse();

    List<Reminder> findByCowCowIdAndIsCompletedFalse(Long cowId);

    List<Reminder> findByReminderType(String reminderType);

    @Query("SELECT r FROM Reminder r WHERE r.isCompleted = false AND r.startDate <= :today")
    List<Reminder> findDueReminders(@Param("today") LocalDate today);

    List<Reminder> findByFrequency(Reminder.Frequency frequency);

    List<Reminder> findByFarmFarmIdOrderByStartDateDesc(Long farmId);

    List<Reminder> findByFarmFarmIdAndIsCompletedFalse(Long farmId);

    long countByFarmFarmIdAndIsCompletedFalse(Long farmId);

    @Query("SELECT r FROM Reminder r WHERE r.farm.farmId = :farmId AND r.isCompleted = false AND r.startDate <= :today")
    List<Reminder> findDueRemindersByFarm(@Param("farmId") Long farmId, @Param("today") LocalDate today);
}