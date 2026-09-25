package org.example.service;

import org.example.dao.CronjobRequest;
import org.example.dao.CronjobResponse;
import org.example.model.Cronjob;
import org.springframework.web.server.ResponseStatusException;
import org.example.repository.CronjobExecutionRepository;
import org.example.repository.CronjobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CronjobServiceTest {
    @Mock
    private CronjobRepository repository;
    @Mock
    private CronjobExecutionRepository mappingRepository;
    @Mock
    private DynamicCronjobSchedulerService schedulerService;

    private CronjobService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new CronjobService(
                repository, mappingRepository, schedulerService);
    }

    @Test
    void create_validCronjob_trimsAndSaves() {
        CronjobRequest request = request("  Daily test  ", "0 */5 * * * *");
        when(repository.existsByNameIgnoreCase("Daily test")).thenReturn(false);
        when(repository.save(any(Cronjob.class))).thenAnswer(invocation -> {
            Cronjob value = invocation.getArgument(0);
            value.setId(10L);
            return value;
        });

        CronjobResponse response = service.create(request);

        assertEquals(10L, response.getId());
        assertEquals("Daily test", response.getName());
        assertEquals("0 */5 * * * *", response.getCronValue());
        verify(repository).save(any(Cronjob.class));
        verifyNoInteractions(schedulerService);
    }

    @Test
    void create_invalidCronExpression_rejectsRequest() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request("Daily test", "0 99 * * * *")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Invalid cron expression", exception.getReason());
        verify(repository, never()).save(any());
    }

    @Test
    void create_blankCronExpression_rejectsRequest() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request("Daily test", "   ")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Cron expression is required", exception.getReason());
        verify(repository, never()).save(any());
    }

    @Test
    void create_cronExpressionWithoutSixFields_rejectsRequest() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request("Daily test", "*/5 * * * *")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertNotNull(exception.getReason());
        assertTrue(exception.getReason().contains("exactly 6 fields"));
        verify(repository, never()).save(any());
    }

    @Test
    void create_duplicateName_rejectsRequest() {
        when(repository.existsByNameIgnoreCase("Daily test")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        request("Daily test", "0 */5 * * * *")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void create_nullName_rejectsRequest() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(request(null, "0 */5 * * * *")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Cronjob name is required", exception.getReason());
        verifyNoInteractions(repository);
    }

    @Test
    void create_blankName_rejectsRequest() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(request("   ", "0 */5 * * * *")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Cronjob name is required", exception.getReason());
    }

    @Test
    void validateCron_nullExpression_rejectsRequest() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> CronjobService.validateCron(null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Cron expression is required", exception.getReason());
    }

    @Test
    void findById_existingCronjob_returnsResponse() {
        when(repository.findById(1L)).thenReturn(Optional.of(cronjob("Daily")));

        CronjobResponse response = service.findById(1L);

        assertEquals(1L, response.getId());
        assertEquals("Daily", response.getName());
    }

    @Test
    void findById_missingCronjob_returnsNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.findById(1L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Cronjob not found", exception.getReason());
    }

    @Test
    void search_nullKeyword_usesEmptyKeywordAndMapsPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(repository.search("", pageable)).thenReturn(
                new PageImpl<>(Collections.singletonList(cronjob("Daily")), pageable, 1));

        org.springframework.data.domain.Page<CronjobResponse> result = service.search(null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Daily", result.getContent().get(0).getName());
        verify(repository).search("", pageable);
    }

    @Test
    void search_keyword_trimsKeyword() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(repository.search("Daily", pageable)).thenReturn(
                new PageImpl<>(Collections.emptyList(), pageable, 0));

        service.search("  Daily  ", pageable);

        verify(repository).search("Daily", pageable);
    }

    @Test
    void update_changedCronValue_reschedulesCronjob() {
        Cronjob existing = cronjob("Old");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        CronjobResponse response = service.update(
                1L, request("New", "0 */10 * * * *"));

        assertEquals("New", response.getName());
        assertEquals("0 */10 * * * *", response.getCronValue());
        verify(schedulerService).reschedule(1L);
    }

    @Test
    void update_onlyNameChanges_doesNotReschedule() {
        Cronjob existing = cronjob("Old");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        service.update(1L, request("New", "0 */5 * * * *"));

        verifyNoInteractions(schedulerService);
    }

    @Test
    void update_duplicateName_rejectsRequest() {
        Cronjob existing = cronjob("Old");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameIgnoreCaseAndIdNot("New", 1L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.update(1L, request(" New ", "0 */5 * * * *")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Cronjob name already exists", exception.getReason());
        verify(repository, never()).save(any());
    }

    @Test
    void update_missingCronjob_returnsNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.update(1L, request("New", "0 */5 * * * *")));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void delete_cronjobWithMappings_rejectsRequest() {
        Cronjob existing = cronjob("Daily");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(mappingRepository.existsByCronjobId(1L)).thenReturn(true);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.delete(1L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(repository, never()).delete(any());
        verify(schedulerService, never()).cancel(anyLong());
    }

    @Test
    void delete_cronjobWithoutMappings_deletesAndCancelsSchedule() {
        Cronjob existing = cronjob("Daily");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        verify(repository).delete(existing);
        verify(schedulerService).cancel(1L);
    }

    @Test
    void afterCommit_activeTransaction_defersCallbackUntilCommit() {
        Runnable callback = mock(Runnable.class);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        try {
            CronjobService.afterCommit(callback);
            verifyNoInteractions(callback);

            for (TransactionSynchronization synchronization
                    : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(callback).run();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void afterCommit_transactionWithoutSynchronization_runsImmediately() {
        Runnable callback = mock(Runnable.class);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            CronjobService.afterCommit(callback);

            verify(callback).run();
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    private CronjobRequest request(String name, String cronValue) {
        CronjobRequest request = new CronjobRequest();
        request.setName(name);
        request.setCronValue(cronValue);
        return request;
    }

    private Cronjob cronjob(String name) {
        Cronjob cronjob = new Cronjob();
        cronjob.setId(1L);
        cronjob.setName(name);
        cronjob.setCronValue("0 */5 * * * *");
        return cronjob;
    }
}
