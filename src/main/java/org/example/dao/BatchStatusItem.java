package org.example.dao;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class BatchStatusItem {
    @NotNull(message = "Mapping id is required")
    private Long id;

    @NotNull(message = "Expected status is required")
    private Boolean expectedStatus;
}
