package com.cowtrack.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductionRequest {

    @NotNull(message = "Cow ID is required")
    private Long cowId;

    @NotNull(message = "Record date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDate;

    @DecimalMin(value = "0.0", message = "Milk yield cannot be negative")
    private BigDecimal milkLitres;

    @DecimalMin(value = "0.0", message = "Weight cannot be negative")
    private BigDecimal weightKg;
}
