package com.cowtrack.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class VaccinationRequest {

    @NotNull(message = "Cow ID is required")
    private Long cowId;

    @NotBlank(message = "Vaccine name is required")
    private String vaccineName;

    /** Left null when scheduling a future dose. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate administeredDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextDueDate;

    private String vetName;
    private String notes;
}
