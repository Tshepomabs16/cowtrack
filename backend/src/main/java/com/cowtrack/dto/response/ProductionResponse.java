package com.cowtrack.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductionResponse {
    private Long productionId;
    private Long cowId;
    private String cowName;
    private LocalDate recordDate;
    private BigDecimal milkLitres;
    private BigDecimal weightKg;
}
