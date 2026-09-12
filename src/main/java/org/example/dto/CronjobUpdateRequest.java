package org.example.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class CronjobUpdateRequest extends CronjobRequest {
    @NotNull(message = "Cronjob id is required")
    private Long id;
}
