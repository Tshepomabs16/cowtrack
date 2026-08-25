package com.cowtrack.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Settings-page profile edit. The client sends the display name as {@code name},
 * matching the registration form.
 */
@Data
public class ProfileUpdateRequest {

    @JsonAlias("name")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Email(message = "Email should be valid")
    private String email;

    private String phone;
    private String farmName;
    private String location;
    private String timezone;
    private String language;
}
