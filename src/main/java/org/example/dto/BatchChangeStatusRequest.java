package org.example.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class BatchChangeStatusRequest {
    @NotNull(message = "Cronjob id is required")
    private Long cronjobId;

    @NotEmpty(message = "Items must not be empty")
    @Valid
    private List<BatchStatusItem> items;

    @NotNull(message = "New status is required")
    private Boolean status;
}
