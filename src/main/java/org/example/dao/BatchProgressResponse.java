package org.example.dao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BatchProgressResponse {
    private String executeKey;
    private String runId;
    private double percent;
    private String status;
    private BatchExecutionResponse result;

    public BatchProgressResponse(String executeKey, double percent, String status) {
        this(executeKey, null, percent, status, null);
    }

    public BatchProgressResponse(String executeKey, double percent, String status, BatchExecutionResponse result) {
        this(executeKey, null, percent, status, result);
    }
}
