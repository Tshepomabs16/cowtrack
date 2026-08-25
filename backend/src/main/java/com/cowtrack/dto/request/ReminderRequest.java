package com.cowtrack.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ReminderRequest {

    @NotNull(message = "Cow ID is required")
    private Long cowId;

    @NotBlank(message = "Reminder type is required")
    private String reminderType; // "VACCINATION", "DEWORMING", "VET_CHECKUP", "MILKING"

    @NotBlank(message = "Frequency is required")
    private String frequency; // "DAILY", "WEEKLY", "MONTHLY"

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    private String notes;
}