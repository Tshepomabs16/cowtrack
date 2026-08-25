package com.cowtrack.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class HealthRecordResponse {
    private Long healthId;
    private Long cowId;
    private String cowName;
    private String diagnosis;
    private String treatment;
    private String vetName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}