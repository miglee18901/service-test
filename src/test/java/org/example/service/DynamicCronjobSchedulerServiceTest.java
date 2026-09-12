package org.example.service;

import org.example.dto.BaseResponse;
import org.example.dto.UserDetails;
import org.example.entity.Cronjob;
import org.example.entity.CronjobExecution;
import org.example.entity.ExecutionInfo;
import org.example.repository.CronjobExecutionRepository;
import org.example.repository.CronjobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicCronjobSchedulerServiceTest {
    @Mock
    private CronjobRepository cronjobRepository;
    @Mock
    private CronjobExecutionRepository mappingRepository;
    @Mock
    private ExecutionStartService executionStartService;
    @Mock
    private ThreadPoolTaskScheduler taskScheduler;
    @Mock
    private ScheduledFuture<Object> scheduledFuture;

    private DynamicCronjobSchedulerService service;

    @BeforeEach
    void setUp() {
        service = new DynamicCronjobSchedulerService(
                cronjobRepository,
                mappingRepository,
                executionStartService,
                taskScheduler);
    }

    @Test
    void scheduleIfNecessaryShouldScheduleWhenEnabledMappingExists() {
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
    void scheduleIfNecessaryShouldCancelWhenNoEnabledMappingRemains() {
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
    void reloadAllShouldRestoreOnlyCronjobsReturnedByRepository() {
        Cronjob cronjob = cronjob(3L, "0 */10 * * * *");
        when(cronjobRepository.findAllHavingEnabledExecutions())
                .thenReturn(Collections.singletonList(cronjob));
        doReturn(scheduledFuture).when(taskScheduler)
                .schedule(any(Runnable.class), any(Trigger.class));

        service.reloadAll();

        assertTrue(service.isScheduled(3L));
    }

    @Test
    void scheduledRunnableShouldCallStartForEveryEnabledExecution() {
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
        when(executionStartService.start(
                eq(20L), any(UserDetails.class)))
                .thenReturn(new BaseResponse<>(200, "Success", null));

        service.schedule(cronjob);
        runnableCaptor.getValue().run();

        ArgumentCaptor<UserDetails> userCaptor =
                ArgumentCaptor.forClass(UserDetails.class);
        verify(executionStartService).start(eq(20L), userCaptor.capture());
        assertEquals("cronjob:1", userCaptor.getValue().getUsername());
    }

    private Cronjob cronjob(Long id, String cronValue) {
        Cronjob cronjob = new Cronjob();
        cronjob.setId(id);
        cronjob.setName("Cronjob " + id);
        cronjob.setCronValue(cronValue);
        return cronjob;
    }
}
