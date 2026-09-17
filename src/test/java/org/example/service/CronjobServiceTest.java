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
import org.springframework.http.HttpStatus;

import java.util.Optional;

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
    void createShouldTrimAndSaveValidCronjob() {
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
    void createShouldRejectInvalidCronExpression() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request("Daily test", "0 99 * * * *")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Invalid cron expression", exception.getReason());
        verify(repository, never()).save(any());
    }

    @Test
    void createShouldRejectBlankCronExpression() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request("Daily test", "   ")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Cron expression is required", exception.getReason());
        verify(repository, never()).save(any());
    }

    @Test
    void createShouldRejectCronExpressionWithoutSixFields() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request("Daily test", "*/5 * * * *")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertNotNull(exception.getReason());
        assertTrue(exception.getReason().contains("exactly 6 fields"));
        verify(repository, never()).save(any());
    }

    @Test
    void createShouldRejectDuplicateName() {
        when(repository.existsByNameIgnoreCase("Daily test")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        request("Daily test", "0 */5 * * * *")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void updateShouldRescheduleWhenCronValueChanges() {
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
    void updateShouldNotRescheduleWhenOnlyNameChanges() {
        Cronjob existing = cronjob("Old");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        service.update(1L, request("New", "0 */5 * * * *"));

        verifyNoInteractions(schedulerService);
    }

    @Test
    void deleteShouldRejectCronjobThatStillHasMappings() {
        Cronjob existing = cronjob("Daily");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(mappingRepository.existsByCronjobId(1L)).thenReturn(true);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.delete(1L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(repository, never()).delete(any());
        verify(schedulerService, never()).cancel(anyLong());
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
