package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.client.MockExecutionApiClient;
import org.example.dto.BaseResponse;
import org.example.dto.UserDetails;
import org.example.entity.ExecutionElement;
import org.example.entity.ExecutionInfo;
import org.example.repository.ExecutionInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionStartServiceTest {
    @Mock private ExecutionInfoRepository repository;
    @Mock private ExecutionInfoHistoryService historyService;
    @Mock private ExecutionUserLogService userLogService;
    @Mock private MockExecutionApiClient apiClient;
    @Mock private TaskExecutor taskExecutor;

    private ExecutionStartService service;

    @BeforeEach
    void setUp() {
        service = new ExecutionStartService(repository, historyService, userLogService,
                apiClient, new ObjectMapper(), taskExecutor);
    }

    @Test
    void shouldRejectMissingExecution() {
        BaseResponse<Map<String, String>> response = service.start(1L, new UserDetails("user"));
        assertEquals(HttpStatus.CONFLICT.value(), response.getStatus());
        assertEquals("Execution Info not exists", response.getMessage());
        verifyNoInteractions(apiClient, taskExecutor);
    }

    @Test
    void shouldRejectExecutionWithoutElements() {
        ExecutionInfo info = new ExecutionInfo();
        info.setExecutionElements(Collections.emptyList());
        when(repository.findOne(1L)).thenReturn(info);
        BaseResponse<?> response = service.start(1L, new UserDetails("user"));
        assertEquals("You must add at least 1 execution element", response.getMessage());
    }

    @Test
    void shouldRejectRunningExecution() {
        ExecutionInfo info = validExecution(1L);
        info.setState(1);
        when(repository.findOne(1L)).thenReturn(info);
        BaseResponse<?> response = service.start(1L, new UserDetails("user"));
        assertEquals("You can't start this execution", response.getMessage());
    }

    @Test
    void shouldFailWhenApiResponseIsInvalidJson() {
        when(repository.findOne(1L)).thenReturn(validExecution(1L));
        when(historyService.checkOldExecutionHistory(any())).thenReturn(10L);
        when(apiClient.start(1L)).thenReturn(ResponseEntity.ok("invalid-json"));
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.start(1L, new UserDetails("user")));
        assertEquals("Call API error, please try again!", exception.getMessage());
    }

    @Test
    void shouldReturnCallApiErrorWhenStatusIsNotOk() {
        when(repository.findOne(1L)).thenReturn(validExecution(1L));
        when(historyService.checkOldExecutionHistory(any())).thenReturn(10L);
        when(apiClient.start(1L)).thenReturn(new ResponseEntity<>("{}", HttpStatus.BAD_REQUEST));
        BaseResponse<?> response = service.start(1L, new UserDetails("user"));
        assertEquals("Call API error, please try again!", response.getMessage());
    }

    @Test
    void shouldFailWhenApiDoesNotReturnPositiveId() {
        when(repository.findOne(1L)).thenReturn(validExecution(1L));
        when(historyService.checkOldExecutionHistory(any())).thenReturn(10L);
        when(apiClient.start(1L)).thenReturn(ResponseEntity.ok("{\"id\":0}"));
        BaseResponse<?> response = service.start(1L, new UserDetails("user"));
        assertEquals(HttpStatus.CONFLICT.value(), response.getStatus());
        assertEquals("Start fail !", response.getMessage());
    }

    @Test
    void shouldFailWhenReturnedExecutionDoesNotExist() {
        when(repository.findOne(1L)).thenReturn(validExecution(1L));
        when(repository.findOne(2L)).thenReturn(null);
        when(historyService.checkOldExecutionHistory(any())).thenReturn(10L);
        when(apiClient.start(1L)).thenReturn(ResponseEntity.ok("{\"id\":2}"));
        BaseResponse<?> response = service.start(1L, new UserDetails("user"));
        assertEquals("Start fail !", response.getMessage());
    }

    @Test
    void shouldSaveLogAndSubmitUploadTaskOnSuccess() {
        ExecutionInfo initial = validExecution(1L);
        ExecutionInfo returned = validExecution(2L);
        when(repository.findOne(1L)).thenReturn(initial);
        when(repository.findOne(2L)).thenReturn(returned);
        when(repository.save(returned)).thenReturn(returned);
        when(historyService.checkOldExecutionHistory(initial)).thenReturn(10L);
        when(historyService.getMaxExecutionHistory()).thenReturn(20L);
        when(apiClient.start(1L)).thenReturn(ResponseEntity.ok("{\"id\":2}"));

        BaseResponse<Map<String, String>> response = service.start(1L, new UserDetails("tester"));

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertEquals("10", response.getData().get("executionInfoHistoryId"));
        assertEquals("20", response.getData().get("maxExecutionHistory"));
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