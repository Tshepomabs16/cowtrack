package com.cowtrack.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeviceRegistrationRequest {

    /** As printed on the collar. Unique across the system. */
    @NotBlank(message = "Serial number is required")
    @Size(max = 100, message = "Serial number must be 100 characters or fewer")
    private String serialNumber;

    /** Optional: a collar can be registered before it is fitted. */
    private Long cowId;
}
