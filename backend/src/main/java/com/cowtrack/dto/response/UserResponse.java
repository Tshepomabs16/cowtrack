package com.cowtrack.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private String phone;
    private String farmName;
    private String location;
    private String timezone;
    private String language;
    private LocalDateTime createdAt;
    private Integer totalCows;
    private Integer activeAlerts;
}