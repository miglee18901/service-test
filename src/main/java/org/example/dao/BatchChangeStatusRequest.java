package org.example.dao;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class BatchChangeStatusRequest {
    @NotEmpty(message = "Items must not be empty")
    @Valid
    private List<BatchStatusItem> items;

    @NotNull(message = "New status is required")
    private Boolean status;
}
