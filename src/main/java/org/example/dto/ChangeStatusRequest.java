package org.example.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class ChangeStatusRequest {
    @NotNull(message = "Cronjob execution id is required")
    private Long id;

    @NotNull(message = "Expected status is required")
    private Boolean expectedStatus;

    @NotNull(message = "New status is required")
    private Boolean status;
}
