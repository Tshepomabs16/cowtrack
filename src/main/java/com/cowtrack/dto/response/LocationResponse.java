package com.cowtrack.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LocationResponse {
    private Long locationId;
    private Long cowId;
    private String cowName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal accuracy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordedAt;
}
