package com.cowtrack.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * One entry in a bulk cow update. Every field except the id is optional and only
 * the ones present are applied, so callers can change a single attribute across
 * many animals without resending each full record.
 */
@Data
public class BulkCowUpdateRequest {

    @NotNull(message = "Cow ID is required for each entry")
    @JsonAlias("id")
    private Long cowId;

    private String tagId;
    private String name;
    private String breed;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private Long caretakerId;
}
