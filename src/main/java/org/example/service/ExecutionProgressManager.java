package org.example.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ExecutionProgressManager {
    private static final ExecutionProgressManager INSTANCE = new ExecutionProgressManager();
    private final Map<String, BatchProgressTracker<?>> trackers = new ConcurrentHashMap<>();

    private ExecutionProgressManager() {
    }

    public static ExecutionProgressManager getInstance() {
        return INSTANCE;
    }

    public void register(String runId, BatchProgressTracker<?> tracker) {
        if (runId != null && tracker != null) trackers.put(runId.trim(), tracker);
    }

    public BatchProgressTracker<?> get(String runId) {
        return runId == null ? null : trackers.get(runId.trim());
    }

    public void increment(String runId) {
        BatchProgressTracker<?> tracker = get(runId);
        if (tracker != null) tracker.increment();
    }

    public void remove(String runId) {
        if (runId != null) trackers.remove(runId.trim());
    }

    public void clear() {
        trackers.clear();
    }
}
