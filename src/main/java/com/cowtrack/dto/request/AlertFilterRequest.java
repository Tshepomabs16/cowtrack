package com.cowtrack.dto.request;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AlertFilterRequest {
    private Long cowId;
    private List<String> alertTypes;
    private Boolean isResolved;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer page = 0;
    private Integer size = 20;
}