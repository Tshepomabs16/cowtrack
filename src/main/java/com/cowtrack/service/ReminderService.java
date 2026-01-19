package com.cowtrack.service;

import com.cowtrack.dto.request.ReminderRequest;
import com.cowtrack.dto.response.ReminderResponse;
import java.util.List;

public interface ReminderService {
    ReminderResponse createReminder(ReminderRequest request);
    ReminderResponse getReminderById(Long reminderId);
    List<ReminderResponse> getRemindersByCow(Long cowId);
    List<ReminderResponse> getActiveReminders();
    ReminderResponse updateReminder(Long reminderId, ReminderRequest request);
    ReminderResponse markAsCompleted(Long reminderId);
    void deleteReminder(Long reminderId);
    List<ReminderResponse> getDueReminders();
    void checkAndGenerateReminders();
}