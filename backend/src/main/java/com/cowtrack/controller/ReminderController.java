package com.cowtrack.controller;

import com.cowtrack.dto.request.ReminderRequest;
import com.cowtrack.dto.response.ReminderResponse;
import com.cowtrack.service.ReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController extends BaseController {

    private final ReminderService reminderService;

    @PostMapping
    public ResponseEntity<?> createReminder(@Valid @RequestBody ReminderRequest request) {
        ReminderResponse reminder = reminderService.createReminder(request);
        return created(reminder);
    }

    @GetMapping("/{reminderId}")
    public ResponseEntity<?> getReminderById(@PathVariable Long reminderId) {
        ReminderResponse reminder = reminderService.getReminderById(reminderId);
        return success(reminder);
    }

    @GetMapping("/cow/{cowId}")
    public ResponseEntity<?> getRemindersByCow(@PathVariable Long cowId) {
        List<ReminderResponse> reminders = reminderService.getRemindersByCow(cowId);
        return success(reminders);
    }

    /** Every reminder. The client lists them all and groups them itself. */
    @GetMapping
    public ResponseEntity<?> getAllReminders() {
        return success(reminderService.getAllReminders());
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveReminders() {
        List<ReminderResponse> reminders = reminderService.getActiveReminders();
        return success(reminders);
    }

    @GetMapping("/due")
    public ResponseEntity<?> getDueReminders() {
        List<ReminderResponse> reminders = reminderService.getDueReminders();
        return success(reminders);
    }

    @PutMapping("/{reminderId}")
    public ResponseEntity<?> updateReminder(
            @PathVariable Long reminderId,
            @Valid @RequestBody ReminderRequest request) {
        ReminderResponse reminder = reminderService.updateReminder(reminderId, request);
        return success("Reminder updated successfully", reminder);
    }

    /**
     * PUT, matching the client and the resolve operations on alerts: completing
     * is idempotent. This accepted only POST, so completing a reminder from the
     * UI returned 405 and had never worked.
     */
    @PutMapping("/{reminderId}/complete")
    public ResponseEntity<?> markReminderAsCompleted(@PathVariable Long reminderId) {
        ReminderResponse reminder = reminderService.markAsCompleted(reminderId);
        return success("Reminder marked as completed", reminder);
    }

    @DeleteMapping("/{reminderId}")
    public ResponseEntity<?> deleteReminder(@PathVariable Long reminderId) {
        reminderService.deleteReminder(reminderId);
        return success("Reminder deleted successfully", null);
    }

    @PostMapping("/check-due")
    public ResponseEntity<?> checkDueReminders() {
        reminderService.checkAndGenerateReminders();
        return success("Due reminders checked", null);
    }
}