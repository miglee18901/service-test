package org.example.dao;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartExecutionRequest {
    private Integer handleTestCaseMin = 10;
    private Long timeout = 300000L;
    private Long timeSleep = 1000L;
}
