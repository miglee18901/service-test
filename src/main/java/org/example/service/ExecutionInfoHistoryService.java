package org.example.service;

import org.example.model.ExecutionInfo;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class ExecutionInfoHistoryService {
    private final AtomicLong sequence = new AtomicLong(System.currentTimeMillis());

    public Long checkOldExecutionHistory(ExecutionInfo executionInfo) {
        return sequence.incrementAndGet();
    }

    public long getMaxExecutionHistory() {
        return sequence.get();
    }
}
