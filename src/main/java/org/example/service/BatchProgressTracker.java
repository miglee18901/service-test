package org.example.service;

import org.example.dao.BatchExecutionResponse;
import org.example.dao.BatchProgressResponse;

import java.util.concurrent.atomic.AtomicInteger;

public class BatchProgressTracker {
    private final String executeKey;
    private final int total;
    private final long createdAt = System.currentTimeMillis();
    private final AtomicInteger processed = new AtomicInteger(0);
    private volatile boolean completed = false;
    private volatile BatchExecutionResponse result;

    public BatchProgressTracker(String executeKey, int total) {
        this.executeKey = executeKey;
        this.total = Math.max(0, total);
    }

    public void increment() {
        processed.incrementAndGet();
    }

    public void complete(BatchExecutionResponse result) {
        this.result = result;
        this.completed = true;
    }

    public void complete() {
        this.completed = true;
    }

    public BatchProgressResponse toProgressResponse() {
        BatchProgressResponse response;
        if (completed) {
            response = new BatchProgressResponse(executeKey, 100.0, "COMPLETED", result);
        } else {
            int current = processed.get();
            double pct = total > 0 ? (current * 100.0) / total : 0.0;
            double rounded = Math.min(99.9, Math.round(pct * 10.0) / 10.0);
            response = new BatchProgressResponse(executeKey, rounded, "RUNNING", null);
        }
        response.setRunId(executeKey);
        return response;
    }

    public String getExecuteKey() {
        return executeKey;
    }

    public int getTotal() {
        return total;
    }

    public int getProcessed() {
        return processed.get();
    }

    public boolean isCompleted() {
        return completed;
    }

    public BatchExecutionResponse getResult() {
        return result;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
