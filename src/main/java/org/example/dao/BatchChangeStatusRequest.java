package org.example.dao;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class BatchChangeStatusRequest {
    @NotNull(message = "New status is required")
    private Boolean status;
}
