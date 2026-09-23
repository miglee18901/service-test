package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.client.MockExecutionApiClient;
import org.example.controller.BaseResponse;
import org.example.dao.UserDetails;
import org.example.dao.ExecuteVimProperties;
import org.example.model.ExecutionElement;
import org.example.model.ExecutionInfo;
import org.example.repository.ExecutionInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExecutionInfoServiceTest {
    @Mock private ExecutionInfoRepository repository;
    @Mock private ExecutionInfoHistoryService historyService;
    @Mock private ExecutionUserLogService userLogService;
    @Mock private MockExecutionApiClient apiClient;
    @Mock private TaskExecutor taskExecutor;

    private ExecutionInfoService service;
    private ExecuteVimProperties executeVimProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        executeVimProperties = mock(ExecuteVimProperties.class);
        service = new ExecutionInfoService(repository, historyService, userLogService,
                apiClient, new ObjectMapper(), taskExecutor, executeVimProperties);
    }

    @Test
    void listExecutionVim_configuredOptions_returnsOptions() {
        when(executeVimProperties.getOptions()).thenReturn(java.util.Arrays.asList("vim-a", "vim-b"));
        BaseResponse response = service.listExecutionVim();
        assertEquals(200, response.getStatus());
        assertEquals("Get list execute vim success", response.getMessage());
        assertEquals(java.util.Arrays.asList("vim-a", "vim-b"), response.getData());
    }

    @Test
    void start_missingExecution_returnsError() {
        BaseResponse response = service.start(1L, new UserDetails("user"));
        assertEquals(HttpStatus.CONFLICT.value(), response.getStatus());
        assertEquals("Execution Info not exists", response.getMessage());
        verifyNoInteractions(apiClient, taskExecutor);
    }

    @Test
    void start_executionWithoutElements_returnsError() {
        ExecutionInfo info = new ExecutionInfo();
        info.setExecutionElements(Collections.emptyList());
        when(repository.findOne(1L)).thenReturn(info);
        BaseResponse response = service.start(1L, new UserDetails("user"));
        assertEquals("You must add at least 1 execution element", response.getMessage());
    }

    @Test
    void start_runningExecution_returnsError() {
        ExecutionInfo info = validExecution(1L);
        info.setState(1);
        when(repository.findOne(1L)).thenReturn(info);
        BaseResponse response = service.start(1L, new UserDetails("user"));
        assertEquals("You can't start this execution", response.getMessage());
    }

    @Test
    void start_invalidApiJson_returnsError() {
        when(repository.findOne(1L)).thenReturn(validExecution(1L));
        when(historyService.checkOldExecutionHistory(any())).thenReturn(10L);
        when(apiClient.start(1L)).thenReturn(ResponseEntity.ok("invalid-json"));
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.start(1L, new UserDetails("user")));
        assertEquals("Call API error, please try again!", exception.getMessage());
    }

    @Test
    void start_nonOkApiStatus_returnsCallApiError() {
        when(repository.findOne(1L)).thenReturn(validExecution(1L));
        when(historyService.checkOldExecutionHistory(any())).thenReturn(10L);
        when(apiClient.start(1L)).thenReturn(new ResponseEntity<>("{}", HttpStatus.BAD_REQUEST));
        BaseResponse response = service.start(1L, new UserDetails("user"));
        assertEquals("Call API error, please try again!", response.getMessage());
    }

    @Test
    void start_nonPositiveApiId_returnsError() {
        when(repository.findOne(1L)).thenReturn(validExecution(1L));
        when(historyService.checkOldExecutionHistory(any())).thenReturn(10L);
        when(apiClient.start(1L)).thenReturn(ResponseEntity.ok("{\"id\":0}"));
        BaseResponse response = service.start(1L, new UserDetails("user"));
        assertEquals(HttpStatus.CONFLICT.value(), response.getStatus());
        assertEquals("Start fail !", response.getMessage());
    }

    @Test
    void start_missingReturnedExecution_returnsError() {
        when(repository.findOne(1L)).thenReturn(validExecution(1L));
        when(repository.findOne(2L)).thenReturn(null);
        when(historyService.checkOldExecutionHistory(any())).thenReturn(10L);
        when(apiClient.start(1L)).thenReturn(ResponseEntity.ok("{\"id\":2}"));
        BaseResponse response = service.start(1L, new UserDetails("user"));
        assertEquals("Start fail !", response.getMessage());
    }

    @Test
    void start_validExecution_savesLogAndSubmitsUpload() {
        ExecutionInfo initial = validExecution(1L);
        ExecutionInfo returned = validExecution(2L);
        when(repository.findOne(1L)).thenReturn(initial);
        when(repository.findOne(2L)).thenReturn(returned);
        when(repository.save(returned)).thenReturn(returned);
        when(historyService.checkOldExecutionHistory(initial)).thenReturn(10L);
        when(historyService.getMaxExecutionHistory()).thenReturn(20L);
        when(apiClient.start(1L)).thenReturn(ResponseEntity.ok("{\"id\":2}"));

        BaseResponse response = service.start(1L, new UserDetails("tester"));

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertEquals("10", ((Map<?, ?>) response.getData()).get("executionInfoHistoryId"));
        assertEquals("20", ((Map<?, ?>) response.getData()).get("maxExecutionHistory"));
        assertNotNull(returned.getStartExecutionTime());
        verify(userLogService).log(returned, ExecutionInfoAction.START, "tester");
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(taskExecutor).execute(task.capture());
        assertNotNull(task.getValue());
    }

    private ExecutionInfo validExecution(Long id) {
        ExecutionInfo info = new ExecutionInfo();
        info.setId(id);
        info.setState(0);
        info.setExecutionElements(Collections.singletonList(new ExecutionElement()));
        return info;
    }
}
