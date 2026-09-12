package org.example.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class CronjobExecutionUpdateRequest extends CronjobExecutionRequest {
    @NotNull(message = "Cronjob execution id is required")
    private Long id;
}
