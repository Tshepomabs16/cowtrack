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

    // Find reminders for a cow
    List<Reminder> findByCowCowIdOrderByStartDateDesc(Long cowId);

    // Find active (incomplete) reminders
    List<Reminder> findByIsCompletedFalse();

    // Find active reminders for a cow
    List<Reminder> findByCowCowIdAndIsCompletedFalse(Long cowId);

    // Find reminders by type
    List<Reminder> findByReminderType(String reminderType);

    // Find reminders due today or earlier
    @Query("SELECT r FROM Reminder r WHERE r.isCompleted = false AND r.startDate <= :today")
    List<Reminder> findDueReminders(@Param("today") LocalDate today);

    // Find reminders by frequency
    List<Reminder> findByFrequency(Reminder.Frequency frequency);
}