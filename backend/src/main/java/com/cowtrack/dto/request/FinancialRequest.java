package com.cowtrack.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FinancialRequest {

    @NotBlank(message = "Entry type is required")
    @Pattern(regexp = "(?i)REVENUE|COST", message = "Entry type must be REVENUE or COST")
    private String entryType;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private String category;
    private String description;
    private Long cowId;

    @NotNull(message = "Record date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDate;
}
