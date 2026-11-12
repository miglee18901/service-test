package org.example.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.dao.*;
import org.example.task.MultiRestCallable;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadExecuteManager {
    private static final Logger logger = LogManager.getLogger(ThreadExecuteManager.class);

    private static ThreadExecuteManager instance;
    private final Map<Long, List<BatchParameter>> mapServerParamSet = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> mapExecuteKey = new ConcurrentHashMap<>();
    private final Map<String, BatchExecutionResponse> mapExecuteKeyToResponse = new ConcurrentHashMap<>();
    private final Map<String, List<ExecuteObj>> mapUserToListExecute = new ConcurrentHashMap<>();
    private final ExecutorService executorService;

    private ThreadExecuteManager() {
        this.executorService = Executors.newFixedThreadPool(100);
    }

    public static synchronized ThreadExecuteManager getInstance() {
        if (instance == null) {
            instance = new ThreadExecuteManager();
        }
        return instance;
    }

    public synchronized BatchExecutionResponse addThreads(String executeKey, List<ExecuteObj> lstExecute) {
        BatchExecutionResponse batchResponse = ThreadExecuteManager.getInstance().getMapExecuteKeyToResponse().get(executeKey);
        if (batchResponse == null) {
            logger.error("NOT FOUND INIT BATCH RESPONSE");
            return null;
        }

        try {
            logger.info("Start add callable for executeKey={}, lstExecute={}", executeKey, lstExecute.size());

            ThreadExecuteManager.getInstance().getMapUserToListExecute().put(executeKey.trim(), lstExecute);

            // add task
            CompletionService<ExecuteObj> completionService = new ExecutorCompletionService<>(executorService);
            for (int i = 0; i < lstExecute.size(); i++) {
                String threadName = lstExecute.get(i).getCommand().getName() + "_thread_" + (i + 1);
                MultiRestCallable task = new MultiRestCallable(threadName, lstExecute.get(i));
                completionService.submit(task);
            }

            batchResponse.setListSuccessResponse(new ArrayList<>());
            batchResponse.setListFailResponse(new ArrayList<>());
            // wait to all task finish
            for (int i = 0; i < lstExecute.size(); i++) {
                Future<ExecuteObj> future = completionService.take();
                try {
                    ExecuteObj executeObj = future.get();
                    batchResponse.getListSuccessResponse().addAll(executeObj.getSuccessList());
                    batchResponse.getListFailResponse().addAll(executeObj.getFailList());
                    batchResponse.setTotalSent(batchResponse.getTotalSent() + executeObj.getTotalMsg());
                    batchResponse.setTotalSuccess(batchResponse.getTotalSuccess() + executeObj.getSuccessList().size());
                    batchResponse.setTotalError(batchResponse.getTotalError() + executeObj.getFailList().size());
                    logger.info("batchResponse current: {}", batchResponse);
                } catch (Exception e) {
                    logger.error("FAILED AT GET RESPONSE FROM CALLABLE THREAD");
                    logger.error(e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("FAILED AT ADD CALLABLE THREAD");
            logger.error(e.getMessage(), e);
        } finally {
            ThreadExecuteManager.getInstance().getMapUserToListExecute().remove(executeKey);
            batchResponse.setEndTime(System.currentTimeMillis());
            batchResponse.setProcessTime(batchResponse.getEndTime() - batchResponse.getStartTime());
        }
        return batchResponse;
    }

    public boolean checkUserIsRunningBatch(String username) {
        for (String key : mapUserToListExecute.keySet()) {
            if (username.equals(key.split("_")[0])) {
                return true;
            }
        }
        return false;
    }

    public Map<String, List<ExecuteObj>> getMapUserToListExecute() {
        return mapUserToListExecute;
    }

    public Map<Long, List<BatchParameter>> getMapServerParamSet() {
        return mapServerParamSet;
    }

    public Map<String, AtomicInteger> getMapExecuteKey() {
        return mapExecuteKey;
    }

    public Map<String, BatchExecutionResponse> getMapExecuteKeyToResponse() {
        return mapExecuteKeyToResponse;
    }

    // Lifecycle helper: the screenshots do not show application shutdown.
    public static synchronized void shutdown() {
        if (instance != null) {
            instance.executorService.shutdownNow();
            instance.mapServerParamSet.clear();
            instance.mapExecuteKey.clear();
            instance.mapExecuteKeyToResponse.clear();
            instance.mapUserToListExecute.clear();
            ExecutionProgressManager.getInstance().clear();
            instance = null;
        }
    }
}
