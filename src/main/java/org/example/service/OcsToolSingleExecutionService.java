package org.example.service;

import org.example.client.MockBatchExecutionClient;
import org.example.constants.HttpRequestConstants;
import org.example.constants.ServiceConstants;
import org.example.dao.*;
import org.example.utils.ConnectionManager;
import org.example.utils.HttpClientUtil;
import org.example.utils.OcsToolUtil;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.net.URI;
import java.util.*;

@Service
public class OcsToolSingleExecutionService {
    private final MockBatchExecutionClient client;
    private final Validator validator;
    private final org.example.config.ApplicationProperties properties;

    public OcsToolSingleExecutionService(MockBatchExecutionClient client, Validator validator,
                                         org.example.config.ApplicationProperties properties) {
        this.client = client;
        this.validator = validator;
        this.properties = properties;
    }

    public SingleExecutionResponse executeSingle(SingleExecutionRequest request, String username) {
        if (request == null || request.getRunId() == null || request.getRunId().trim().isEmpty()) {
            return failure("RUN_ID_REQUIRED", System.currentTimeMillis());
        }
        String runId = request.getRunId().trim();
        BatchProgressTracker<SingleExecutionResponse> tracker = new BatchProgressTracker<>(4);
        ExecutionProgressManager.getInstance().register(runId, tracker);
        SingleExecutionResponse result = executeSingleTracked(request, username, runId);
        tracker.complete(result);
        return result;
    }

    private SingleExecutionResponse executeSingleTracked(SingleExecutionRequest request, String username, String runId) {
        long requestTime = System.currentTimeMillis();
        SingleExecutionResponse response = new SingleExecutionResponse();
        if (request == null) {
            return finish(response, HttpRequestConstants.ErrorCode.Fail, "COMMAND_NOT_FOUND", requestTime);
        }
        try {
            Set<ConstraintViolation<SingleExecutionRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                return finish(response, HttpRequestConstants.ErrorCode.Fail,
                        violations.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().findFirst().get(), requestTime);
            }
        } catch (RuntimeException exception) {
            return finish(response, HttpRequestConstants.ErrorCode.Fail, "SINGLE_EXECUTE_FAILED", requestTime);
        }
        ExecutionProgressManager.getInstance().increment(runId);
        if (request.getCommand() == null) {
            return finish(response, HttpRequestConstants.ErrorCode.Fail, "COMMAND_NOT_FOUND", requestTime);
        }
        long startTime = System.currentTimeMillis();
        CommandRequest commandRequest = buildCommandRequest(request);
        ExecutionProgressManager.getInstance().increment(runId);
        boolean tokenAvailable = checkToken(request.getAuthServer());
        ExecutionProgressManager.getInstance().increment(runId);
        SingleExecutionResponse executionResponse = tokenAvailable
                ? executeSingle(commandRequest, request.getServer(), request.getAuthServer(),
                        request.getCommand().getSuccessCode(), 0, request, runId)
                : failure("GET_TOKEN_FAIL", requestTime);
        long endTime = System.currentTimeMillis();
        executionResponse.setServerAddress(request.getServer().getHost());
        executionResponse.setRequestTime(startTime);
        executionResponse.setResponseTime(endTime);
        executionResponse.setProcessTime(endTime - startTime);
        return executionResponse;
    }

    private CommandRequest buildCommandRequest(SingleExecutionRequest request) {
        try {
            return OcsToolUtil.convertXmlToCommandRequest(OcsToolUtil.parseParameter(
                    request.getCommand().getCode(), request.getListParameter()));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private SingleExecutionResponse executeSingle(CommandRequest commandRequest, OcsVcServerDTO server,
                                                   BatchExecutionRequest.AuthServer authServer,
                                                   String successCode, int retryTime, SingleExecutionRequest request, String runId) {
        SingleExecutionResponse response = new SingleExecutionResponse();
        URI apiUrl = null;
        try {
            if (commandRequest == null) return failure("COMMAND_CODE_NOT_VALID", 0);
            try {
                apiUrl = HttpClientUtil.getSafeHttpUri(server.getHost(), commandRequest.getPath());
                if (apiUrl == null) return failure("CANNOT_DETECT_SERVER", 0);
            } catch (IllegalArgumentException exception) {
                return failure("CANNOT_DETECT_SERVER", 0);
            }
            RestTemplate restTemplate = ConnectionManager.getInstance().createRestTemplate(
                    properties.getExecuteConfig().getTimeoutExecuteMilli(), properties.getExecuteConfig().getTimeoutExecuteMilli());
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setBearerAuth("mock-access-token");
            if (commandRequest.getHeader() != null) commandRequest.getHeader().forEach(headers::add);
            HttpEntity<String> entity = new HttpEntity<>(commandRequest.getBody(), headers);
            ExecutionProgressManager.getInstance().increment(runId);
            ConnectionManager.getInstance().setMockParameter(request.getMockParameter() == null
                    ? new BatchParameter() : request.getMockParameter());
            ResponseEntity<String> result;
            try {
                result = ConnectionManager.getInstance().sentHttp(restTemplate, apiUrl,
                        HttpMethod.valueOf(commandRequest.getMethod().toUpperCase(Locale.ROOT)), entity, String.class);
            } finally {
                ConnectionManager.getInstance().clearMockParameter();
            }
            response.setResponseData(result.getBody());
            if (result.getStatusCode() == HttpStatus.OK) {
                response.setRetn(HttpRequestConstants.ErrorCode.Success);
                response.setDescription(OcsToolUtil.checkResultCode(result.getBody(), successCode)
                        ? "Success" : "SINGLE_EXECUTE_FAILED");
            } else {
                response.setRetn(HttpRequestConstants.ErrorCode.Fail);
                response.setDescription("GET_TOKEN_FAIL");
            }
            return response;
        } catch (HttpClientErrorException.Unauthorized unauthorized) {
            if (retryTime > 0) return failure("GET_TOKEN_FAIL_LIMIT", 0);
            if (checkToken(authServer, true)) {
                return executeSingle(commandRequest, server, authServer, successCode, retryTime + 1, request, runId);
            }
            return failure("GET_TOKEN_FAIL", 0);
        } catch (Exception exception) {
            return failure("SINGLE_EXECUTE_FAILED", 0);
        }
    }

    private boolean checkToken(BatchExecutionRequest.AuthServer authServer) {
        return authServer != null && client.checkToken(authServer);
    }

    private boolean checkToken(BatchExecutionRequest.AuthServer authServer, boolean refresh) {
        return checkToken(authServer);
    }

    private SingleExecutionResponse failure(String description, long start) {
        return finish(new SingleExecutionResponse(), HttpRequestConstants.ErrorCode.Fail, description,
                start == 0 ? System.currentTimeMillis() : start);
    }

    private SingleExecutionResponse finish(SingleExecutionResponse response, String retn, String description, long start) {
        long end = System.currentTimeMillis();
        response.setRetn(retn);
        response.setDescription(description);
        response.setRequestTime(start);
        response.setResponseTime(end);
        response.setProcessTime(end - start);
        return response;
    }
}
