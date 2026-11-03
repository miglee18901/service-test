package org.example.service;

import org.example.dao.*;
import org.example.model.Cronjob;
import org.example.model.CronjobExecution;
import org.example.model.ExecutionElement;
import org.example.model.ExecutionInfo;
import org.springframework.web.server.ResponseStatusException;
import org.example.repository.CronjobExecutionRepository;
import org.example.repository.ExecutionInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        MockitoAnnotations.initMocks(this);
        service = new CronjobExecutionService(
                repository,
                cronjobService,
                executionInfoRepository,
                schedulerService);
    }

    @Test
    void create_validMapping_savesAndSchedulesCronjob() {
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
    void create_executionWithoutElements_rejectsRequest() {
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
    void create_executionWithNullElements_rejectsRequest() {
        ExecutionInfo execution = execution(10L, true);
        execution.setExecutionElements(null);
        when(cronjobService.getEntity(1L)).thenReturn(cronjob(1L));
        when(executionInfoRepository.findOne(10L)).thenReturn(execution);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(request(1L, 10L, true)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void create_missingExecution_returnsNotFound() {
        when(cronjobService.getEntity(1L)).thenReturn(cronjob(1L));
        when(executionInfoRepository.findOne(10L)).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(request(1L, 10L, true)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Execution info not found", exception.getReason());
    }

    @Test
    void create_executionAlreadyAssigned_rejectsRequest() {
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
    void findById_existingMapping_returnsResponse() {
        when(repository.findById(100L)).thenReturn(Optional.of(mapping(100L, 1L, 10L, true)));

        CronjobExecutionResponse response = service.findById(100L);

        assertEquals(100L, response.getId());
        assertEquals(1L, response.getCronjobId());
        assertEquals(10L, response.getExecutionInfoId());
    }

    @Test
    void findById_missingMapping_returnsNotFound() {
        when(repository.findById(100L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.findById(100L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Cronjob execution not found", exception.getReason());
    }

    @Test
    void search_nullKeyword_mapsResponsePage() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(repository.search("", 1L, 10L, true, pageable)).thenReturn(
                new PageImpl<>(Collections.singletonList(mapping(100L, 1L, 10L, true)), pageable, 1));

        org.springframework.data.domain.Page<CronjobExecutionResponse> result =
                service.search(null, 1L, 10L, true, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(100L, result.getContent().get(0).getId());
    }

    @Test
    void search_keyword_trimsKeyword() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(repository.search("job", null, null, null, pageable)).thenReturn(
                new PageImpl<>(Collections.emptyList(), pageable, 0));

        service.search("  job  ", null, null, null, pageable);

        verify(repository).search("job", null, null, null, pageable);
    }

    @Test
    void update_validMapping_savesAndSchedulesOldAndNewCronjobs() {
        CronjobExecution existing = mapping(100L, 1L, 10L, true);
        Cronjob newCronjob = cronjob(2L);
        ExecutionInfo newExecution = execution(20L, true);
        when(repository.findById(100L)).thenReturn(Optional.of(existing));
        when(cronjobService.getEntity(2L)).thenReturn(newCronjob);
        when(executionInfoRepository.findOne(20L)).thenReturn(newExecution);
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        CronjobExecutionResponse response = service.update(100L, request(2L, 20L, false));

        assertEquals(2L, response.getCronjobId());
        assertEquals(20L, response.getExecutionInfoId());
        assertFalse(response.getStatus());
        verify(schedulerService).scheduleIfNecessary(1L);
        verify(schedulerService).scheduleIfNecessary(2L);
    }

    @Test
    void update_executionAssignedToAnotherMapping_returnsConflict() {
        when(repository.findById(100L)).thenReturn(Optional.of(mapping(100L, 1L, 10L, true)));
        when(cronjobService.getEntity(2L)).thenReturn(cronjob(2L));
        when(executionInfoRepository.findOne(20L)).thenReturn(execution(20L, true));
        when(repository.existsByExecutionInfoIdAndIdNot(20L, 100L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.update(100L, request(2L, 20L, false)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void delete_existingMapping_deletesFlushesAndSchedulesCronjob() {
        CronjobExecution existing = mapping(100L, 1L, 10L, true);
        when(repository.findById(100L)).thenReturn(Optional.of(existing));

        service.delete(100L);

        verify(repository).delete(existing);
        verify(repository).flush();
        verify(schedulerService).scheduleIfNecessary(1L);
    }

    @Test
    void delete_missingMapping_returnsNotFound() {
        when(repository.findById(100L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.delete(100L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(repository, never()).delete(any());
    }

    @Test
    void changeStatus_matchingCurrentStatus_updatesToExpectedStatusAndSyncsScheduler() {
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
    void changeStatus_staleSession_returnsConflict() {
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
    void batchStatus_statusOnly_updatesAllMappingsAndSyncsOnce() {
        when(cronjobService.getEntity(1L)).thenReturn(cronjob(1L));
        when(repository.findByCronjobId(1L)).thenReturn(Arrays.asList(
                mapping(100L, 1L, 10L, false),
                mapping(101L, 1L, 11L, false)));

        java.util.List<CronjobExecutionResponse> responses =
                service.changeAllStatuses(1L, batchRequest(false));

        assertEquals(2, responses.size());
        assertTrue(responses.stream().noneMatch(CronjobExecutionResponse::getStatus));
        org.mockito.InOrder order = inOrder(repository);
        order.verify(repository).updateStatusByCronjobId(1L, false);
        order.verify(repository).findByCronjobId(1L);
        verify(repository, never()).updateStatusIfMatches(anyLong(), anyBoolean(), anyBoolean());
        verify(schedulerService).scheduleIfNecessary(1L);
    }

    @Test
    void batchStatus_enableAll_updatesByCronjobId() {
        when(cronjobService.getEntity(2L)).thenReturn(cronjob(2L));
        when(repository.findByCronjobId(2L)).thenReturn(
                Collections.singletonList(mapping(102L, 2L, 12L, true)));

        java.util.List<CronjobExecutionResponse> responses =
                service.changeAllStatuses(2L, batchRequest(true));

        assertTrue(responses.get(0).getStatus());
        verify(repository).updateStatusByCronjobId(2L, true);
        verify(schedulerService).scheduleIfNecessary(2L);
    }

    @Test
    void batchStatus_emptyCronjob_succeedsWithEmptyList() {
        when(cronjobService.getEntity(1L)).thenReturn(cronjob(1L));
        when(repository.findByCronjobId(1L)).thenReturn(Collections.emptyList());

        assertTrue(service.changeAllStatuses(1L, batchRequest(false)).isEmpty());

        verify(repository).updateStatusByCronjobId(1L, false);
        verify(schedulerService).scheduleIfNecessary(1L);
    }

    @Test
    void batchStatus_missingCronjob_doesNotUpdateMappings() {
        when(cronjobService.getEntity(1L)).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Cronjob not found"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.changeAllStatuses(1L, batchRequest(false)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verifyNoInteractions(repository, schedulerService);
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
            boolean status, boolean expectedStatus) {
        ChangeStatusRequest request = new ChangeStatusRequest();
        request.setExpectedStatus(expectedStatus);
        request.setStatus(status);
        return request;
    }

    private BatchChangeStatusRequest batchRequest(boolean status) {
        BatchChangeStatusRequest request = new BatchChangeStatusRequest();
        request.setStatus(status);
        return request;
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
