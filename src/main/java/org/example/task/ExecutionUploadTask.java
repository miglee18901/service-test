package org.example.task;

import org.example.model.ExecutionInfo;
import org.example.repository.ExecutionInfoRepository;

import java.util.Date;

public class ExecutionUploadTask implements Runnable {
    private final ExecutionInfo executionInfo;
    private final ExecutionInfoRepository repository;
    private final int handleTestCaseMin10;
    private final long timeout;
    private final long timeSleep;

    public ExecutionUploadTask(ExecutionInfo executionInfo, ExecutionInfoRepository repository, int handleTestCaseMin10, long timeout, long timeSleep) {
        this.executionInfo = executionInfo;
        this.repository = repository;
        this.handleTestCaseMin10 = handleTestCaseMin10;
        this.timeout = timeout;
        this.timeSleep = timeSleep;
    }

    @Override
    public void run() {
        try {
            long delay = Math.min(Math.max(timeSleep, 0), Math.min(timeout, 5000));
            Thread.sleep(delay);
            repository.findById(executionInfo.getId()).ifPresent(info -> {
                info.setCurrentState(handleTestCaseMin10 > 0 ? 1 : 0);
                info.setCompletedTime(new Date());
                repository.save(info);
            });
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
