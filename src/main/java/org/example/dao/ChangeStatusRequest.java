package org.example.dao;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class ChangeStatusRequest {
    @NotNull(message = "Desired status is required")
    private Boolean expectedStatus;

    @NotNull(message = "Current status is required")
    private Boolean status;
}
