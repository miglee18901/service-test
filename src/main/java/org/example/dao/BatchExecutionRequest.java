package org.example.dao;

import lombok.Getter;
import lombok.Setter;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class BatchExecutionRequest {
    @NotNull @Valid
    private OcsVcServerDTO server;
    @NotNull @Valid
    private OcsVcCommandDTO command;
    @NotNull @Valid
    private AuthServer authServer;
    private String username;
    private String reason;
    @Min(1) @Max(100)
    private int numOfThread = 1;
    @NotEmpty @Size(max = 10000) @Valid
    private List<@NotNull BatchParameter> listBatchParam = new ArrayList<>();

    public Long getCommandId() {
        return command == null ? null : command.getId();
    }

    @Getter @Setter
    public static class AuthServer {
        private boolean tokenAvailable = true;
    }
}
