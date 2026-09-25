package org.example.service;

import org.example.controller.BaseResponse;
import org.example.dao.UserDetails;
import org.example.model.Cronjob;
import org.example.model.CronjobExecution;
import org.example.model.ExecutionInfo;
import org.example.repository.CronjobExecutionRepository;
import org.example.repository.CronjobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Collections;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DynamicCronjobSchedulerServiceTest {
    @Mock
    private CronjobRepository cronjobRepository;
    @Mock
    private CronjobExecutionRepository mappingRepository;
    @Mock
    private ExecutionInfoService executionInfoService;
    @Mock
    private ThreadPoolTaskScheduler taskScheduler;
    @Mock
    private ScheduledFuture<Object> scheduledFuture;

    private DynamicCronjobSchedulerService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DynamicCronjobSchedulerService(
                cronjobRepository,
                mappingRepository,
                executionInfoService,
                taskScheduler);
    }

    @Test
    void scheduleIfNecessary_enabledMapping_schedulesCronjob() {
        Cronjob cronjob = cronjob(1L, "*/5 * * * * *");
        when(mappingRepository.existsByCronjobIdAndStatusTrue(1L))
                .thenReturn(true);
        when(cronjobRepository.findById(1L)).thenReturn(Optional.of(cronjob));
        doReturn(scheduledFuture).when(taskScheduler)
                .schedule(any(Runnable.class), any(Trigger.class));

        service.scheduleIfNecessary(1L);

        assertTrue(service.isScheduled(1L));
        verify(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void scheduleIfNecessary_noEnabledMapping_cancelsCronjob() {
        Cronjob cronjob = cronjob(1L, "*/5 * * * * *");
        doReturn(scheduledFuture).when(taskScheduler)
                .schedule(any(Runnable.class), any(Trigger.class));
        service.schedule(cronjob);
        when(mappingRepository.existsByCronjobIdAndStatusTrue(1L))
                .thenReturn(false);

        service.scheduleIfNecessary(1L);

        assertFalse(service.isScheduled(1L));
        verify(scheduledFuture).cancel(false);
    }

    @Test
    void scheduleIfNecessary_alreadyScheduled_doesNotScheduleAgain() {
        Cronjob cronjob = cronjob(1L, "*/5 * * * * *");
        when(mappingRepository.existsByCronjobIdAndStatusTrue(1L)).thenReturn(true);
        when(cronjobRepository.findById(1L)).thenReturn(Optional.of(cronjob));
        doReturn(scheduledFuture).when(taskScheduler)
                .schedule(any(Runnable.class), any(Trigger.class));

        service.scheduleIfNecessary(1L);
        service.scheduleIfNecessary(1L);

        verify(cronjobRepository, times(1)).findById(1L);
        verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void scheduleIfNecessary_missingCronjob_doesNothing() {
        when(mappingRepository.existsByCronjobIdAndStatusTrue(1L)).thenReturn(true);
        when(cronjobRepository.findById(1L)).thenReturn(Optional.empty());

        service.scheduleIfNecessary(1L);

        assertFalse(service.isScheduled(1L));
        verifyNoInteractions(taskScheduler);
    }

    @Test
    void reschedule_existingTask_cancelsAndSchedulesAgain() {
        Cronjob cronjob = cronjob(1L, "*/5 * * * * *");
        when(mappingRepository.existsByCronjobIdAndStatusTrue(1L)).thenReturn(true);
        when(cronjobRepository.findById(1L)).thenReturn(Optional.of(cronjob));
        doReturn(scheduledFuture).when(taskScheduler)
                .schedule(any(Runnable.class), any(Trigger.class));
        service.schedule(cronjob);

        service.reschedule(1L);

        verify(scheduledFuture).cancel(false);
        verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void schedule_invalidCronExpression_throwsException() {
        Cronjob cronjob = cronjob(1L, "invalid");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.schedule(cronjob));

        assertTrue(exception.getMessage().contains("Invalid cron expression"));
        verifyNoInteractions(taskScheduler);
    }

    @Test
    void schedule_schedulerReturnsNull_throwsException() {
        Cronjob cronjob = cronjob(1L, "*/5 * * * * *");
        when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class))).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.schedule(cronjob));

        assertEquals("Cannot schedule cronjob 1", exception.getMessage());
        assertFalse(service.isScheduled(1L));
    }

    @Test
    void cancel_unknownCronjob_doesNothing() {
        service.cancel(99L);

        verifyNoInteractions(taskScheduler, scheduledFuture);
    }

    @Test
    void reloadAll_repositoryCronjobs_restoresOnlyReturnedCronjobs() {
        Cronjob cronjob = cronjob(3L, "0 */10 * * * *");
        when(cronjobRepository.findAllHavingEnabledExecutions())
                .thenReturn(Collections.singletonList(cronjob));
        doReturn(scheduledFuture).when(taskScheduler)
                .schedule(any(Runnable.class), any(Trigger.class));

        service.reloadAll();

        assertTrue(service.isScheduled(3L));
    }

    @Test
    void reloadAll_invalidCronjob_continuesSchedulingRemainingCronjobs() {
        Cronjob invalid = cronjob(1L, "invalid");
        Cronjob valid = cronjob(2L, "0 */10 * * * *");
        when(cronjobRepository.findAllHavingEnabledExecutions())
                .thenReturn(Arrays.asList(invalid, valid));
        doReturn(scheduledFuture).when(taskScheduler)
                .schedule(any(Runnable.class), any(Trigger.class));

        assertDoesNotThrow(() -> service.reloadAll());

        assertFalse(service.isScheduled(1L));
        assertTrue(service.isScheduled(2L));
    }

    @Test
    void scheduledRunnable_enabledExecutions_startsEachExecution() {
        Cronjob cronjob = cronjob(1L, "*/5 * * * * *");
        ExecutionInfo execution = new ExecutionInfo();
        execution.setId(20L);
        CronjobExecution mapping = new CronjobExecution();
        mapping.setCronjob(cronjob);
        mapping.setExecutionInfo(execution);
        mapping.setStatus(true);

        ArgumentCaptor<Runnable> runnableCaptor =
                ArgumentCaptor.forClass(Runnable.class);
        doReturn(scheduledFuture).when(taskScheduler)
                .schedule(runnableCaptor.capture(), any(Trigger.class));
        when(mappingRepository.findByCronjobIdAndStatusTrue(1L))
                .thenReturn(Collections.singletonList(mapping));
        when(executionInfoService.start(
                eq(20L), any(UserDetails.class)))
                .thenReturn(new BaseResponse(HttpStatus.OK.value(), "Success", null));

        service.schedule(cronjob);
        runnableCaptor.getValue().run();

        ArgumentCaptor<UserDetails> userCaptor =
                ArgumentCaptor.forClass(UserDetails.class);
        verify(executionInfoService).start(eq(20L), userCaptor.capture());
        assertEquals("cronjob:1", userCaptor.getValue().getUsername());
    }

    @Test
    void scheduledRunnable_noEnabledExecutions_cancelsTask() {
        Cronjob cronjob = cronjob(1L, "*/5 * * * * *");
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doReturn(scheduledFuture).when(taskScheduler)
                .schedule(runnableCaptor.capture(), any(Trigger.class));
        when(mappingRepository.findByCronjobIdAndStatusTrue(1L))
                .thenReturn(Collections.emptyList());
        service.schedule(cronjob);

        runnableCaptor.getValue().run();

        verify(scheduledFuture).cancel(false);
        assertFalse(service.isScheduled(1L));
        verifyNoInteractions(executionInfoService);
    }

    @Test
    void scheduledRunnable_oneExecutionFails_continuesWithNextExecution() {
        Cronjob cronjob = cronjob(1L, "*/5 * * * * *");
        CronjobExecution first = mapping(cronjob, 20L);
        CronjobExecution second = mapping(cronjob, 21L);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doReturn(scheduledFuture).when(taskScheduler)
                .schedule(runnableCaptor.capture(), any(Trigger.class));
        when(mappingRepository.findByCronjobIdAndStatusTrue(1L))
                .thenReturn(Arrays.asList(first, second));
        when(executionInfoService.start(eq(20L), any(UserDetails.class)))
                .thenThrow(new RuntimeException("start failed"));
        when(executionInfoService.start(eq(21L), any(UserDetails.class)))
                .thenReturn(new BaseResponse(HttpStatus.OK.value(), "Success", null));
        service.schedule(cronjob);

        assertDoesNotThrow(() -> runnableCaptor.getValue().run());

        verify(executionInfoService).start(eq(20L), any(UserDetails.class));
        verify(executionInfoService).start(eq(21L), any(UserDetails.class));
    }

    @Test
    void isScheduled_cancelledOrCompletedFuture_returnsFalse() {
        Cronjob cronjob = cronjob(1L, "*/5 * * * * *");
        doReturn(scheduledFuture).when(taskScheduler)
                .schedule(any(Runnable.class), any(Trigger.class));
        service.schedule(cronjob);
        when(scheduledFuture.isCancelled()).thenReturn(true);
        assertFalse(service.isScheduled(1L));

        when(scheduledFuture.isCancelled()).thenReturn(false);
        when(scheduledFuture.isDone()).thenReturn(true);
        assertFalse(service.isScheduled(1L));
    }

    @Test
    void cancelAll_cancelsEveryTaskAndClearsRegistry() {
        Cronjob first = cronjob(1L, "*/5 * * * * *");
        Cronjob second = cronjob(2L, "*/10 * * * * *");
        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> secondFuture = mock(ScheduledFuture.class);
        doReturn(scheduledFuture, secondFuture).when(taskScheduler)
                .schedule(any(Runnable.class), any(Trigger.class));
        service.schedule(first);
        service.schedule(second);

        service.cancelAll();

        verify(scheduledFuture).cancel(false);
        verify(secondFuture).cancel(false);
        assertFalse(service.isScheduled(1L));
        assertFalse(service.isScheduled(2L));
    }

    private CronjobExecution mapping(Cronjob cronjob, Long executionId) {
        ExecutionInfo execution = new ExecutionInfo();
        execution.setId(executionId);
        CronjobExecution mapping = new CronjobExecution();
        mapping.setCronjob(cronjob);
        mapping.setExecutionInfo(execution);
        mapping.setStatus(true);
        return mapping;
    }

    private Cronjob cronjob(Long id, String cronValue) {
        Cronjob cronjob = new Cronjob();
        cronjob.setId(id);
        cronjob.setName("Cronjob " + id);
        cronjob.setCronValue(cronValue);
        return cronjob;
    }
}
