package com.cowtrack.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import java.time.LocalDate;

@Data
public class HealthRecordRequest {

    @NotNull(message = "Cow ID is required")
    private Long cowId;

    @NotBlank(message = "Diagnosis is required")
    private String diagnosis;

    private String treatment;
    private String vetName;

    @NotNull(message = "Record date is required")
    @PastOrPresent(message = "Record date cannot be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDate;
}