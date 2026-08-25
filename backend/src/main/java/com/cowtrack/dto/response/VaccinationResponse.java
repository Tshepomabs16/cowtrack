package com.cowtrack.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VaccinationResponse {
    private Long vaccinationId;
    private Long cowId;
    private String cowName;
    private String vaccineName;
    private LocalDate administeredDate;
    private LocalDate nextDueDate;
    private String vetName;
    private String notes;
    /** Derived from the dates; see Vaccination#getStatus. */
    private String status;
}
