package org.example.service;

import lombok.Data;
import org.example.dao.BatchProgressResponse;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class BatchProgressTracker<T> {
    private final int total;
    private final long createdAt = System.currentTimeMillis();
    private final AtomicInteger processed = new AtomicInteger(0);
    private volatile boolean completed = false;
    private volatile T result;

    public BatchProgressTracker(int total) {
        this.total = Math.max(0, total);
    }

    public void increment() {
        processed.incrementAndGet();
    }

    public void complete(T result) {
        this.result = result;
        this.completed = true;
    }

    public void complete() {
        this.completed = true;
    }

    public BatchProgressResponse<T> toProgressResponse() {
        BatchProgressResponse<T> response;
        if (completed) {
            response = new BatchProgressResponse<>(100.0, "COMPLETED", result);
        } else {
            int current = processed.get();
            double pct = total > 0 ? (current * 100.0) / total : 0.0;
            double rounded = Math.min(99.9, Math.round(pct * 10.0) / 10.0);
            response = new BatchProgressResponse<>(rounded, "RUNNING", null);
        }
        return response;
    }
}
