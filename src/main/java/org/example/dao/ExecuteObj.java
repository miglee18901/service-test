package org.example.dao;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class ExecuteObj {
    private String executeKey;
    private String progressRunId;
    private long startTime;
    private long endTime;
    private String finishStatus;
    private Integer status;
    private String description;
    private OcsVcCommandDTO command;
    private OcsVcServerDTO server;
    private String authToken;
    private List<OcsVcParameterDTO> listParameter;
    private List<String> listParamString;
    private List<BatchParameter> listBatchParam;
    private String username;
    private String reason;
    private TpsEntity tpsEntity;
    private int connectionTimeout;
    private int readTimeout;
    private int totalMsg;
    private int totalError;
    private List<ExecuteResponse> successList = new ArrayList<>();
    private List<ExecuteResponse> failList = new ArrayList<>();
}
