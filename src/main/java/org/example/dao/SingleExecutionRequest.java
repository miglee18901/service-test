package org.example.dao;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class SingleExecutionRequest {
    @NotBlank
    private String runId;
    @NotNull @Valid
    private OcsVcServerDTO server;
    @NotNull @Valid
    private OcsVcCommandDTO command;
    @NotNull @Valid
    private BatchExecutionRequest.AuthServer authServer = new BatchExecutionRequest.AuthServer();
    @Valid
    private java.util.List<@NotNull OcsVcParameterDTO> listParameter = new java.util.ArrayList<>();
    private BatchParameter mockParameter = new BatchParameter();
    private String reason;
}
