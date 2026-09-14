package org.example.service;

import org.example.dto.*;
import org.example.entity.Cronjob;
import org.example.entity.CronjobExecution;
import org.example.entity.ExecutionElement;
import org.example.entity.ExecutionInfo;
import org.springframework.web.server.ResponseStatusException;
import org.example.repository.CronjobExecutionRepository;
import org.example.repository.ExecutionInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CronjobExecutionServiceTest {
    @Mock
    private CronjobExecutionRepository repository;
    @Mock
    private CronjobService cronjobService;
    @Mock
    private ExecutionInfoRepository executionInfoRepository;
    @Mock
    private DynamicCronjobSchedulerService schedulerService;

    private CronjobExecutionService service;

    @BeforeEach
    void setUp() {
        service = new CronjobExecutionService(
                repository,
                cronjobService,
                executionInfoRepository,
                schedulerService);
    }

    @Test
    void createShouldSaveMappingAndScheduleCronjob() {
        Cronjob cronjob = cronjob(1L);
        ExecutionInfo execution = execution(10L, true);
        CronjobExecutionRequest request = request(1L, 10L, true);
        when(cronjobService.getEntity(1L)).thenReturn(cronjob);
        when(executionInfoRepository.findOne(10L)).thenReturn(execution);
        when(repository.saveAndFlush(any(CronjobExecution.class)))
                .thenAnswer(invocation -> {
                    CronjobExecution value = invocation.getArgument(0);
                    value.setId(100L);
                    return value;
                });

        CronjobExecutionResponse response = service.create(request);

        assertEquals(100L, response.getId());
        assertEquals(1L, response.getCronjobId());
        assertEquals(10L, response.getExecutionInfoId());
        assertTrue(response.getStatus());
        verify(schedulerService).scheduleIfNecessary(1L);
    }

    @Test
    void createShouldRejectExecutionWithoutElements() {
        Cronjob cronjob = cronjob(1L);
        ExecutionInfo execution = execution(10L, false);
        when(cronjobService.getEntity(1L)).thenReturn(cronjob);
        when(executionInfoRepository.findOne(10L)).thenReturn(execution);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request(1L, 10L, true)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createShouldRejectExecutionAlreadyAssignedToCronjob() {
        Cronjob cronjob = cronjob(1L);
        ExecutionInfo execution = execution(10L, true);
        when(cronjobService.getEntity(1L)).thenReturn(cronjob);
        when(executionInfoRepository.findOne(10L)).thenReturn(execution);
        when(repository.existsByExecutionInfoId(10L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request(1L, 10L, true)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(
                "Execution already belongs to another cronjob",
                exception.getReason());
    }

    @Test
    void changeStatusShouldUpdateConditionallyAndSyncScheduler() {
        CronjobExecution before = mapping(100L, 1L, 10L, true);
        CronjobExecution after = mapping(100L, 1L, 10L, false);
        when(repository.findById(100L))
                .thenReturn(Optional.of(before), Optional.of(after));
        when(repository.updateStatusIfMatches(100L, true, false))
                .thenReturn(1);

        CronjobExecutionResponse response = service.changeStatus(
                100L, statusRequest(true, false));

        assertFalse(response.getStatus());
        verify(schedulerService).scheduleIfNecessary(1L);
    }

    @Test
    void changeStatusShouldReturnConflictForStaleSession() {
        CronjobExecution mapping = mapping(100L, 1L, 10L, false);
        when(repository.findById(100L)).thenReturn(Optional.of(mapping));
        when(repository.updateStatusIfMatches(100L, true, false))
                .thenReturn(0);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.changeStatus(
                        100L, statusRequest(true, false)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getReason().contains("reload the page"));
        verifyNoInteractions(schedulerService);
    }

    @Test
    void batchStatusShouldRejectIncompleteClientSnapshot() {
        when(cronjobService.getEntity(1L)).thenReturn(cronjob(1L));
        when(repository.findByCronjobId(1L)).thenReturn(Arrays.asList(
                mapping(100L, 1L, 10L, true),
                mapping(101L, 1L, 11L, false)));
        BatchChangeStatusRequest request =
                batchRequest(false, item(100L, true));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.changeAllStatuses(1L, request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(repository, never()).updateStatusIfMatches(
                anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void batchStatusShouldUpdateEveryMappingAndSyncSchedulerOnce() {
        CronjobExecution first = mapping(100L, 1L, 10L, true);
        CronjobExecution second = mapping(101L, 1L, 11L, false);
        CronjobExecution firstAfter = mapping(100L, 1L, 10L, false);
        CronjobExecution secondAfter = mapping(101L, 1L, 11L, false);
        when(cronjobService.getEntity(1L)).thenReturn(cronjob(1L));
        when(repository.findByCronjobId(1L))
                .thenReturn(
                        Arrays.asList(first, second),
                        Arrays.asList(firstAfter, secondAfter));
        when(repository.updateStatusIfMatches(anyLong(), anyBoolean(), eq(false)))
                .thenReturn(1);
        BatchChangeStatusRequest request = batchRequest(
                false, item(100L, true), item(101L, false));

        java.util.List<CronjobExecutionResponse> responses =
                service.changeAllStatuses(1L, request);

        assertEquals(2, responses.size());
        assertTrue(responses.stream().noneMatch(CronjobExecutionResponse::getStatus));
        verify(repository, times(2)).updateStatusIfMatches(
                anyLong(), anyBoolean(), eq(false));
        verify(schedulerService).scheduleIfNecessary(1L);
    }

    private CronjobExecutionRequest request(
            Long cronjobId, Long executionId, boolean status) {
        CronjobExecutionRequest request = new CronjobExecutionRequest();
        request.setCronjobId(cronjobId);
        request.setExecutionInfoId(executionId);
        request.setStatus(status);
        return request;
    }

    private ChangeStatusRequest statusRequest(
            boolean expectedStatus, boolean status) {
        ChangeStatusRequest request = new ChangeStatusRequest();
        request.setExpectedStatus(expectedStatus);
        request.setStatus(status);
        return request;
    }

    private BatchChangeStatusRequest batchRequest(
            boolean status, BatchStatusItem... items) {
        BatchChangeStatusRequest request = new BatchChangeStatusRequest();
        request.setStatus(status);
        request.setItems(Arrays.asList(items));
        return request;
    }

    private BatchStatusItem item(Long id, boolean expectedStatus) {
        BatchStatusItem item = new BatchStatusItem();
        item.setId(id);
        item.setExpectedStatus(expectedStatus);
        return item;
    }

    private Cronjob cronjob(Long id) {
        Cronjob cronjob = new Cronjob();
        cronjob.setId(id);
        cronjob.setName("Cronjob " + id);
        cronjob.setCronValue("0 */5 * * * *");
        return cronjob;
    }

    private ExecutionInfo execution(Long id, boolean hasElements) {
        ExecutionInfo execution = new ExecutionInfo();
        execution.setId(id);
        execution.setName("Execution " + id);
        execution.setExecutionElements(
                hasElements
                        ? Collections.singletonList(new ExecutionElement())
                        : Collections.emptyList());
        return execution;
    }

    private CronjobExecution mapping(
            Long id,
            Long cronjobId,
            Long executionId,
            boolean status) {
        CronjobExecution mapping = new CronjobExecution();
        mapping.setId(id);
        mapping.setCronjob(cronjob(cronjobId));
        mapping.setExecutionInfo(execution(executionId, true));
        mapping.setStatus(status);
        return mapping;
    }
}
