package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.controller.BaseResponse;
import org.example.dao.UserDetails;
import org.example.dao.ExecuteVimProperties;
import org.example.model.ExecutionElement;
import org.example.model.ExecutionInfo;
import org.example.repository.ExecutionInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockitoAnnotations;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExecutionInfoServiceTest {
    private static final String EXECUTION_API_URL = "http://execution-api/executions";

    private ExecutionInfoRepository executionInfoRepository;
    private ExecutionInfoHistoryService executionInfoHistoryService;
    private ExecutionUserLogService executionUserLogService;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private TaskExecutor taskExecutor;

    private ExecutionInfoService executionInfoService;
    private ExecuteVimProperties executeVimProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        executionInfoRepository = mock(ExecutionInfoRepository.class);
        executionInfoHistoryService = mock(ExecutionInfoHistoryService.class);
        executionUserLogService = mock(ExecutionUserLogService.class);
        restTemplate = mock(RestTemplate.class);
        objectMapper = mock(ObjectMapper.class);
        taskExecutor = mock(TaskExecutor.class);
        executeVimProperties = mock(ExecuteVimProperties.class);
        executionInfoService = new ExecutionInfoService(executionInfoRepository, executionInfoHistoryService, executionUserLogService,
                restTemplate, EXECUTION_API_URL, objectMapper, taskExecutor, executeVimProperties);
    }

    @Test
    void listExecutionVim_configuredOptions_returnsOptions() {
        when(executeVimProperties.getOptions()).thenReturn(java.util.Arrays.asList("vim-a", "vim-b"));
        BaseResponse response = executionInfoService.listExecutionVim();
        assertEquals(200, response.getStatus());
        assertEquals("Get list execute vim success", response.getMessage());
        assertEquals(java.util.Arrays.asList("vim-a", "vim-b"), response.getData());
    }

    @Test
    void start_missingExecution_returnsError() {
        BaseResponse response = executionInfoService.start(1L, new UserDetails("user"));
        assertEquals(HttpStatus.CONFLICT.value(), response.getStatus());
        assertEquals("Execution Info not exists", response.getMessage());
        verifyNoInteractions(restTemplate, taskExecutor);
    }

    @Test
    void start_executionWithoutElements_returnsError() {
        ExecutionInfo info = new ExecutionInfo();
        info.setExecutionElements(Collections.emptyList());
        when(executionInfoRepository.findOne(1L)).thenReturn(info);
        BaseResponse response = executionInfoService.start(1L, new UserDetails("user"));
        assertEquals("You must add at least 1 execution element", response.getMessage());
    }

    @Test
    void start_runningExecution_returnsError() {
        ExecutionInfo info = validExecution(1L);
        info.setState(1);
        when(executionInfoRepository.findOne(1L)).thenReturn(info);
        BaseResponse response = executionInfoService.start(1L, new UserDetails("user"));
        assertEquals("You can't start this execution", response.getMessage());
    }

    @Test
    void start_invalidApiJson_returnsError() throws JsonProcessingException {
        when(executionInfoRepository.findOne(1L)).thenReturn(validExecution(1L));
        when(executionInfoHistoryService.checkOldExecutionHistory(any())).thenReturn(10L);
        mockApiResponse(ResponseEntity.ok("invalid-json"));
        when(objectMapper.readValue(
                anyString(),
                ArgumentMatchers.<TypeReference<Map<String, Object>>>any()))
                .thenThrow(new JsonProcessingException("Invalid JSON") {
                });
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> executionInfoService.start(1L, new UserDetails("user")));
        assertEquals("Call API error, please try again!", exception.getMessage());
    }

    @Test
    void start_nonOkApiStatus_returnsCallApiError() throws JsonProcessingException {
        when(executionInfoRepository.findOne(1L)).thenReturn(validExecution(1L));
        when(executionInfoHistoryService.checkOldExecutionHistory(any())).thenReturn(10L);
        mockApiResponse(new ResponseEntity<>("{}", HttpStatus.BAD_REQUEST));
        mockParsedResponse(Collections.emptyMap());
        BaseResponse response = executionInfoService.start(1L, new UserDetails("user"));
        assertEquals("Call API error, please try again!", response.getMessage());
    }

    @Test
    void start_nonPositiveApiId_returnsError() throws JsonProcessingException {
        when(executionInfoRepository.findOne(1L)).thenReturn(validExecution(1L));
        when(executionInfoHistoryService.checkOldExecutionHistory(any())).thenReturn(10L);
        mockApiResponse(ResponseEntity.ok("{\"id\":0}"));
        mockParsedResponse(apiResponseWithId(0L));
        BaseResponse response = executionInfoService.start(1L, new UserDetails("user"));
        assertEquals(HttpStatus.CONFLICT.value(), response.getStatus());
        assertEquals("Start fail !", response.getMessage());
    }

    @Test
    void start_missingReturnedExecution_returnsError() throws JsonProcessingException {
        when(executionInfoRepository.findOne(1L)).thenReturn(validExecution(1L));
        when(executionInfoRepository.findOne(2L)).thenReturn(null);
        when(executionInfoHistoryService.checkOldExecutionHistory(any())).thenReturn(10L);
        mockApiResponse(ResponseEntity.ok("{\"id\":2}"));
        mockParsedResponse(apiResponseWithId(2L));
        BaseResponse response = executionInfoService.start(1L, new UserDetails("user"));
        assertEquals("Start fail !", response.getMessage());
    }

    @Test
    void start_validExecution_setsStartExecutionTimeBeforeSaving() throws JsonProcessingException {
        ExecutionInfo initial = validExecution(1L);
        ExecutionInfo returned = validExecution(2L);
        when(executionInfoRepository.findOne(1L)).thenReturn(initial);
        when(executionInfoRepository.findOne(2L)).thenReturn(returned);
        when(executionInfoRepository.save(any(ExecutionInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(executionInfoHistoryService.checkOldExecutionHistory(initial)).thenReturn(10L);
        when(executionInfoHistoryService.getMaxExecutionHistory()).thenReturn(20L);
        mockApiResponse(ResponseEntity.ok("{\"id\":2}"));
        mockParsedResponse(apiResponseWithId(2L));
        Date beforeStart = new Date();

        executionInfoService.start(1L, new UserDetails("tester"));

        Date afterStart = new Date();
        ArgumentCaptor<ExecutionInfo> savedExecution = ArgumentCaptor.forClass(ExecutionInfo.class);
        verify(executionInfoRepository).save(savedExecution.capture());
        Date startExecutionTime = savedExecution.getValue().getStartExecutionTime();
        assertNotNull(startExecutionTime);
        assertFalse(startExecutionTime.before(beforeStart));
        assertFalse(startExecutionTime.after(afterStart));
    }

    @Test
    void start_validExecution_savesLogAndSubmitsUpload() throws JsonProcessingException {
        ExecutionInfo initial = validExecution(1L);
        ExecutionInfo returned = validExecution(2L);
        when(executionInfoRepository.findOne(1L)).thenReturn(initial);
        when(executionInfoRepository.findOne(2L)).thenReturn(returned);
        when(executionInfoRepository.save(returned)).thenReturn(returned);
        when(executionInfoHistoryService.checkOldExecutionHistory(initial)).thenReturn(10L);
        when(executionInfoHistoryService.getMaxExecutionHistory()).thenReturn(20L);
        mockApiResponse(ResponseEntity.ok("{\"id\":2}"));
        mockParsedResponse(apiResponseWithId(2L));

        BaseResponse response = executionInfoService.start(1L, new UserDetails("tester"));

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertEquals("10", ((Map<?, ?>) response.getData()).get("executionInfoHistoryId"));
        assertEquals("20", ((Map<?, ?>) response.getData()).get("maxExecutionHistory"));
        assertNotNull(returned.getStartExecutionTime());
        verify(executionUserLogService).log(returned, ExecutionInfoAction.START, "tester");
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(taskExecutor).execute(task.capture());
        assertNotNull(task.getValue());
    }

    private void mockApiResponse(ResponseEntity<String> response) {
        when(restTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                any(Class.class)))
                .thenReturn(response);
    }

    private void mockParsedResponse(Map<String, Object> response) throws JsonProcessingException {
        when(objectMapper.readValue(
                anyString(),
                ArgumentMatchers.<TypeReference<Map<String, Object>>>any()))
                .thenReturn(response);
    }

    private Map<String, Object> apiResponseWithId(Long id) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", id);
        return response;
    }

    private ExecutionInfo validExecution(Long id) {
        ExecutionInfo info = new ExecutionInfo();
        info.setId(id);
        info.setState(0);
        info.setExecutionElements(Collections.singletonList(new ExecutionElement()));
        return info;
    }
}
