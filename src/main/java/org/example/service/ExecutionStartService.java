package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.client.MockExecutionApiClient;
import org.example.dto.BaseResponse;
import org.example.dto.UserDetails;
import org.example.entity.ExecutionInfo;
import org.example.repository.ExecutionInfoRepository;
import org.example.task.ExecutionUploadTask;
import org.springframework.core.task.TaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ExecutionStartService {
    private final ExecutionInfoRepository executionInfoRepository;
    private final ExecutionInfoHistoryService executionInfoHistoryService;
    private final ExecutionUserLogService executionUserLogService;
    private final MockExecutionApiClient restTemplate;
    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;
    private final int handleTestCaseMin10 = 10;
    private final long timeout = 300000L;
    private final long timeSleep = 1000L;

    public ExecutionStartService(
            ExecutionInfoRepository executionInfoRepository,
            ExecutionInfoHistoryService executionInfoHistoryService,
            ExecutionUserLogService executionUserLogService,
            MockExecutionApiClient restTemplate,
            ObjectMapper objectMapper,
            @Qualifier("executionTaskExecutor") TaskExecutor taskExecutor) {
        this.executionInfoRepository = executionInfoRepository;
        this.executionInfoHistoryService = executionInfoHistoryService;
        this.executionUserLogService = executionUserLogService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
    }

    @Transactional
    public BaseResponse<Map<String, String>> start(Long executionInfoId, UserDetails userDetails) {
        ExecutionInfo findExecution = executionInfoRepository.findOne(executionInfoId);
        if (findExecution == null) {
            return new BaseResponse<>(HttpStatus.CONFLICT.value(), "Execution Info not exists", null);
        }
        if (findExecution.getExecutionElements() == null || findExecution.getExecutionElements().isEmpty()) {
            return new BaseResponse<>(HttpStatus.CONFLICT.value(), "You must add at least 1 execution element", null);
        }
        if (Integer.valueOf(1).equals(findExecution.getState())) {
            return new BaseResponse<>(HttpStatus.CONFLICT.value(), "You can't start this execution", null);
        }

        Long executionInfoHistoryId = executionInfoHistoryService.checkOldExecutionHistory(findExecution);
        Map<String, Object> responseAPI;
        ResponseEntity<String> response;
        try {
            // Mocked equivalent of restTemplate.exchange(urlApiExecution + "/" +
            // executionInfoId, HttpMethod.POST, entity, String.class).
            response = restTemplate.start(executionInfoId);
            responseAPI = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Call API error, please try again!");
        }

        if (response.getStatusCode() == HttpStatus.OK) {
            Object responseId = responseAPI.get("id");
            if (responseId != null && Long.parseLong(responseId.toString()) > 0) {
                ExecutionInfo executionInfo = executionInfoRepository.findOne(Long.parseLong(responseId.toString()));
                if (executionInfo == null) {
                    return new BaseResponse<>(HttpStatus.CONFLICT.value(), "Start fail !", null);
                }

                executionInfo.setStartExecutionTime(new Date());
                executionInfo = executionInfoRepository.save(executionInfo);
                executionUserLogService.log(executionInfo, ExecutionInfoAction.START, userDetails.getUsername());
                ExecutionUploadTask task = new ExecutionUploadTask(executionInfo, executionInfoRepository, handleTestCaseMin10, timeout, timeSleep);
                taskExecutor.execute(task);

                Map<String, String> mapResult = new LinkedHashMap<>();
                mapResult.put("executionInfoHistoryId", String.valueOf(executionInfoHistoryId));
                mapResult.put("maxExecutionHistory", String.valueOf(executionInfoHistoryService.getMaxExecutionHistory()));
                return new BaseResponse<>(HttpStatus.OK.value(), "Start successfully !", mapResult);
            } else {
                return new BaseResponse<>(HttpStatus.CONFLICT.value(), "Start fail !", null);
            }
        }
        return new BaseResponse<>(HttpStatus.OK.value(), "Call API error, please try again!", null);
    }
}
