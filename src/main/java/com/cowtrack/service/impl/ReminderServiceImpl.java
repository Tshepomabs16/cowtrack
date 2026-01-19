package com.cowtrack.service.impl;

import com.cowtrack.dto.request.ReminderRequest;
import com.cowtrack.dto.response.ReminderResponse;
import com.cowtrack.entity.Cow;
import com.cowtrack.entity.Reminder;
import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.repository.CowRepository;
import com.cowtrack.repository.ReminderRepository;
import com.cowtrack.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReminderServiceImpl implements ReminderService {

    private final ReminderRepository reminderRepository;
    private final CowRepository cowRepository;

    @Override
    public ReminderResponse createReminder(ReminderRequest request) {
        Cow cow = cowRepository.findById(request.getCowId())
                .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + request.getCowId()));

        Reminder reminder = new Reminder();
        reminder.setCow(cow);
        reminder.setReminderType(request.getReminderType());
        reminder.setFrequency(Reminder.Frequency.valueOf(request.getFrequency().toUpperCase()));
        reminder.setStartDate(request.getStartDate());
        reminder.setIsCompleted(false);
        reminder.setCreatedAt(LocalDateTime.now());

        Reminder savedReminder = reminderRepository.save(reminder);
        log.info("Created reminder for cow {}: {} ({})", cow.getTagId(), request.getReminderType(), request.getFrequency());

        return toResponse(savedReminder);
    }

    @Override
    public ReminderResponse getReminderById(Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with id: " + reminderId));
        return toResponse(reminder);
    }

    @Override
    public List<ReminderResponse> getRemindersByCow(Long cowId) {
        if (!cowRepository.existsById(cowId)) {
            throw new ResourceNotFoundException("Cow not found with id: " + cowId);
        }

        return reminderRepository.findByCowCowIdOrderByStartDateDesc(cowId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReminderResponse> getActiveReminders() {
        return reminderRepository.findByIsCompletedFalse().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReminderResponse updateReminder(Long reminderId, ReminderRequest request) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with id: " + reminderId));

        if (!reminder.getCow().getCowId().equals(request.getCowId())) {
            Cow newCow = cowRepository.findById(request.getCowId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cow not found with id: " + request.getCowId()));
            reminder.setCow(newCow);
        }

        reminder.setReminderType(request.getReminderType());
        reminder.setFrequency(Reminder.Frequency.valueOf(request.getFrequency().toUpperCase()));
        reminder.setStartDate(request.getStartDate());

        Reminder updatedReminder = reminderRepository.save(reminder);
        return toResponse(updatedReminder);
    }

    @Override
    public ReminderResponse markAsCompleted(Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with id: " + reminderId));

        reminder.setIsCompleted(true);
        Reminder updatedReminder = reminderRepository.save(reminder);

        log.info("Reminder {} marked as completed", reminderId);
        return toResponse(updatedReminder);
    }

    @Override
    public void deleteReminder(Long reminderId) {
        if (!reminderRepository.existsById(reminderId)) {
            throw new ResourceNotFoundException("Reminder not found with id: " + reminderId);
        }
        reminderRepository.deleteById(reminderId);
    }

    @Override
    public List<ReminderResponse> getDueReminders() {
        List<Reminder> dueReminders = reminderRepository.findDueReminders(LocalDate.now());
        return dueReminders.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Scheduled(cron = "0 0 8 * * ?") // Run daily at 8 AM
    public void checkAndGenerateReminders() {
        log.info("Checking for due reminders...");

        List<Reminder> dueReminders = reminderRepository.findDueReminders(LocalDate.now());

        for (Reminder reminder : dueReminders) {
            // In production, you would:
            // 1. Send notification (email/SMS/push)
            // 2. Create an alert
            // 3. Log the reminder

            log.info("Reminder due: {} for cow {} - {}",
                    reminder.getReminderType(),
                    reminder.getCow().getName(),
                    reminder.getCow().getTagId());

            // For recurring reminders, update the start date
            if (reminder.getFrequency() != null) {
                LocalDate newStartDate = calculateNextDate(reminder.getStartDate(), reminder.getFrequency());
                reminder.setStartDate(newStartDate);
                reminder.setIsCompleted(false);
                reminderRepository.save(reminder);
            }
        }

        log.info("Found {} due reminders", dueReminders.size());
    }

    private ReminderResponse toResponse(Reminder reminder) {
        ReminderResponse response = new ReminderResponse();
        response.setReminderId(reminder.getReminderId());
        response.setCowId(reminder.getCow().getCowId());
        response.setCowName(reminder.getCow().getName());
        response.setReminderType(reminder.getReminderType());
        response.setFrequency(reminder.getFrequency().name());
        response.setStartDate(reminder.getStartDate());
        response.setIsCompleted(reminder.getIsCompleted());
        response.setCreatedAt(reminder.getCreatedAt());

        // Calculate if overdue
        if (!reminder.getIsCompleted() && reminder.getStartDate().isBefore(LocalDate.now())) {
            response.setIsOverdue(true);
            response.setDaysUntilDue(-ChronoUnit.DAYS.between(reminder.getStartDate(), LocalDate.now()));
        } else if (!reminder.getIsCompleted()) {
            response.setIsOverdue(false);
            response.setDaysUntilDue(ChronoUnit.DAYS.between(LocalDate.now(), reminder.getStartDate()));
        } else {
            response.setIsOverdue(false);
            response.setDaysUntilDue(0L);
        }

        return response;
    }

    private LocalDate calculateNextDate(LocalDate currentDate, Reminder.Frequency frequency) {
        return switch (frequency) {
            case DAILY -> currentDate.plusDays(1);
            case WEEKLY -> currentDate.plusWeeks(1);
            case MONTHLY -> currentDate.plusMonths(1);
            default -> currentDate;
        };
    }
}