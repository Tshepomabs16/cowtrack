package com.cowtrack.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Registration payload for {@code POST /api/auth/register}.
 *
 * <p>The web client posts the display name as {@code name}, so that spelling is
 * accepted alongside {@code fullName}. Extra fields the client sends but the
 * {@link com.cowtrack.entity.User} entity has no column for (phone, farmName)
 * are ignored.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @JsonAlias("name")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "(?i)FARMER|CARETAKER|ADMIN",
            message = "Role must be FARMER, CARETAKER, or ADMIN")
    private String role;
}
