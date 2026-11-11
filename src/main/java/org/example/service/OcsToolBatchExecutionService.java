package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.client.MockBatchExecutionClient;
import org.example.config.ApplicationProperties;
import org.example.constants.HttpRequestConstants;
import org.example.constants.ServiceConstants;
import org.example.dao.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class OcsToolBatchExecutionService {
    private static final Logger logger = LogManager.getLogger(OcsToolBatchExecutionService.class);
    private final ApplicationProperties applicationProperties;
    private final MockBatchExecutionClient client;
    private final Validator validator;
    private final ObjectMapper objectMapper;
    private final Path logDirectory;
    private String accessToken;

    public OcsToolBatchExecutionService(ApplicationProperties applicationProperties,
            MockBatchExecutionClient client, Validator validator, ObjectMapper objectMapper,
            @Value("${app.batch-execution.log-directory:target/batch-logs}") String logDirectory) {
        this.applicationProperties = applicationProperties;
        this.client = client;
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.logDirectory = Paths.get(logDirectory);
    }

    public BatchExecutionResponse executeBatch(BatchExecutionRequest request) {
        logger.info("============== START EXECUTE BATCH ==============");
        checkCondition(request);

        OcsVcServerDTO server = request.getServer();
        String executeKey = request.getUsername() + "_" + request.getCommandId();
        String runId = request.getRunId().trim();

        BatchExecutionResponse response = new BatchExecutionResponse();
        response.setRunId(runId);
        response.setServerName(server.getName());
        response.setHost(server.getHost());
        if (!checkToken(request.getAuthServer())) {
            response.setRetn(ServiceConstants.ExecuteStatus.NOT_AUTHEN + "");
            response.setDescription(ServiceConstants.ExecuteDescription.GET_TOKEN_FAILED);
            return response;
        }

        List<BatchParameter> batchItems = new ArrayList<>(request.getListBatchParam());
        BatchProgressTracker tracker = new BatchProgressTracker(runId, batchItems.size());
        ThreadExecuteManager.getInstance().registerTracker(runId, tracker);

        try {
            ThreadExecuteManager.getInstance().getMapExecuteKeyToResponse().put(executeKey.trim(), response);

            //<editor-fold defaultstate="collapsed" desc="build list request">
            List<BatchParameter> listParamSetCurrent = request.getListBatchParam();
            Map<Long, List<BatchParameter>> mapServerParamSet = ThreadExecuteManager.getInstance().getMapServerParamSet();
            List<BatchParameter> listParamSetOfServer = mapServerParamSet.computeIfAbsent(server.getId(), k -> new ArrayList<>());
            listParamSetOfServer.addAll(listParamSetCurrent);

            int numThread = request.getNumOfThread();
            int numberThreadCurrent = mapServerParamSet.size() * numThread;
            int maxThread = applicationProperties.getExecuteConfig().getMaxThread();

            if (numberThreadCurrent <= maxThread) {
                numThread = Math.min(listParamSetOfServer.size(), numThread);
            } else {
                numThread = Math.min(listParamSetOfServer.size(), (maxThread / mapServerParamSet.size()));
            }

            String des = "\nServer name: " + server.getName() + "\n\n Host: " + server.getHost();

            int pageSize = listParamSetOfServer.size() / numThread;
            // The initialization immediately below pageSize is obscured in the supplied image.
            TpsEntity tpsEntity = new TpsEntity();

            List<ExecuteObj> lstExecute = new ArrayList<>();
            int begin = 0;
            int end = 0;
            for (int i = 0; i < numThread; i++) {
                begin = i * pageSize;
                end = begin + pageSize;
                if (i == numThread - 1) {
                    end = listParamSetOfServer.size();
                }
                List<String> listParamStr = new ArrayList<>();
                List<BatchParameter> listParamSet = new ArrayList<>();
                for (int p = begin; p < end; p++) {
                    listParamSet.add(listParamSetCurrent.get(p));
                }
                logger.info("thread {}, begin {}, end {}, listParamSet size {}", i, begin, end, listParamSet.size());
                long currentTime = System.currentTimeMillis();
                ExecuteObj executeObj = new ExecuteObj();
                executeObj.setExecuteKey(executeKey);
                executeObj.setProgressRunId(runId);
                executeObj.setStartTime(currentTime);
                executeObj.setFinishStatus(ServiceConstants.BatchFinishStatus.RUNNING);
                executeObj.setDescription(des + "\r\n Number of Execution Command: " + listParamSet.size());
                executeObj.setCommand(request.getCommand());
                executeObj.setServer(request.getServer());
                executeObj.setAuthToken(accessToken);
                executeObj.setListParameter(request.getCommand().getListParameter());
                executeObj.setListParamString(listParamStr);
                executeObj.setListBatchParam(listParamSet);
                executeObj.setUsername(request.getUsername());
                executeObj.setReason(request.getReason());
                executeObj.setTpsEntity(tpsEntity);
                executeObj.setConnectionTimeout(applicationProperties.getExecuteConfig().getTimeoutExecuteMilli());
                executeObj.setReadTimeout(applicationProperties.getExecuteConfig().getTimeoutExecuteMilli());
                lstExecute.add(executeObj);
            }
            // </editor-fold>

            // add thread
            response.setStartTime(System.currentTimeMillis());
            BatchExecutionResponse executionResponse = ThreadExecuteManager.getInstance().addThreads(executeKey, lstExecute);

            if (executionResponse == null) {
                logger.error("THREAD EXECUTOR RESPONSE NULL");
                response.setRetn(HttpRequestConstants.ErrorCode.Fail);
                response.setDescription(HttpRequestConstants.Description.BATCH_EXECUTE_FAILED);
                return response;
            } else if (response.getTotalSent() == 0) {
                logger.error("THREAD EXECUTOR RESPONSE NULL");
                response.setRetn(HttpRequestConstants.ErrorCode.Fail);
                response.setDescription(HttpRequestConstants.Description.BATCH_EXECUTE_CANNOT_SENT_REQUEST);
                return response;
            }
            response = executionResponse;
            if (response.getTotalSuccess() == response.getTotalSent()) {
                response.setRetn(HttpRequestConstants.ErrorCode.Success);
                response.setDescription(HttpRequestConstants.Description.Success);
            } else {
                response.setRetn(HttpRequestConstants.ErrorCode.Success);
                response.setDescription(HttpRequestConstants.Description.BATCH_EXECUTE_NOT_SUCCESS_ALL);
            }
            response.setResponseLogFile(writeLogToTextFile(request, response));
            logger.info("batchResponse final: {}", response);
            return response;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            response.setRetn(HttpRequestConstants.ErrorCode.Fail);
            response.setDescription(e.getMessage());
        } finally {
            tracker.complete(response);
            ThreadExecuteManager.getInstance().getMapExecuteKeyToResponse().remove(executeKey);
            ThreadExecuteManager.getInstance().getMapServerParamSet().remove(server.getId());
        }
        return response;
    }

    public BatchProgressResponse getProgress(String executeKey) {
        BatchProgressTracker tracker = ThreadExecuteManager.getInstance().getTracker(executeKey);
        if (tracker != null) {
            BatchProgressResponse progress = tracker.toProgressResponse();
            if (tracker.isCompleted()) {
                ThreadExecuteManager.getInstance().removeTracker(executeKey);
            }
            return progress;
        }
        return new BatchProgressResponse(executeKey, 0.0, "NOT_FOUND");
    }

    // Supporting helpers are not supplied in the screenshots.
    private void checkCondition(BatchExecutionRequest request) {
        if (request == null) throw new IllegalArgumentException("Batch request is required");
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (request.getRunId() == null || request.getRunId().trim().isEmpty()) {
            throw new IllegalArgumentException("Run ID is required");
        }
        Set<ConstraintViolation<BatchExecutionRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().findFirst().get());
        }
        if (ThreadExecuteManager.getInstance().checkUserIsRunningBatch(request.getUsername())) {
            throw new IllegalStateException("User is already running a batch");
        }
    }

    private boolean checkToken(BatchExecutionRequest.AuthServer authServer) {
        if (!client.checkToken(authServer)) return false;
        accessToken = "mock-access-token";
        return true;
    }

    private String writeLogToTextFile(BatchExecutionRequest request, BatchExecutionResponse response) throws IOException {
        Files.createDirectories(logDirectory);
        Path file = Files.createTempFile(logDirectory, "batch-", ".txt");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), response);
        return file.toString();
    }

    @PreDestroy
    public void shutdown() {
        ThreadExecuteManager.shutdown();
    }
}
