package com.cowtrack.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** Animals to move into a camp. Each leaves whichever camp it was in before. */
@Data
public class CampAssignmentRequest {

    @NotEmpty(message = "Name at least one animal")
    @Size(max = 1000, message = "Move at most 1000 animals at a time")
    private List<Long> cowIds;
}
