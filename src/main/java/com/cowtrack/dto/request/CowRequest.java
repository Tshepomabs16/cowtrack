package com.cowtrack.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CowRequest {

    @NotBlank(message = "Tag ID is required")
    @Size(min = 3, max = 50, message = "Tag ID must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9-_]+$", message = "Tag ID can only contain letters, numbers, hyphens, and underscores")
    private String tagId;

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 50, message = "Name must be between 1 and 50 characters")
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private String breed;
    private Long motherId;
    private Long fatherId;
    private Long caretakerId;
}