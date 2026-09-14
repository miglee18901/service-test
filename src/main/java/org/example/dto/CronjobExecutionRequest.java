package org.example.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class CronjobExecutionRequest {
    @NotNull(message = "Cronjob id is required")
    private Long cronjobId;

    @NotNull(message = "Execution info id is required")
    private Long executionInfoId;

    @NotNull(message = "Status is required")
    private Boolean status;

}
