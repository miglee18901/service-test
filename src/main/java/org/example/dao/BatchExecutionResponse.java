package org.example.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class BatchExecutionResponse {
    // HTTP adapter metadata; not part of the OCS result.
    @JsonIgnore
    private int status = 200;
    private String retn;
    private String runId;
    private String description;
    private String serverName;
    private String host;
    private long startTime;
    private long endTime;
    private long processTime;
    private int totalSent;
    private int totalSuccess;
    private int totalError;
    private String responseLogFile;
    private List<ExecuteResponse> listSuccessResponse = new ArrayList<>();
    private List<ExecuteResponse> listFailResponse = new ArrayList<>();
}
