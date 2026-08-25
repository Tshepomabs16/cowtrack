package com.cowtrack.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ReminderResponse {
    private Long reminderId;
    private Long cowId;
    private String cowName;
    private String reminderType;
    private String frequency;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    private Boolean isCompleted;
    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // Calculated fields
    private Boolean isOverdue;
    private Long daysUntilDue;
}