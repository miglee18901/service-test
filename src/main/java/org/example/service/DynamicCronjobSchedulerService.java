package org.example.service;

import org.example.dto.BaseResponse;
import org.example.dto.UserDetails;
import org.example.entity.Cronjob;
import org.example.entity.CronjobExecution;
import org.example.repository.CronjobExecutionRepository;
import org.example.repository.CronjobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class DynamicCronjobSchedulerService {
    private static final Logger log =
            LoggerFactory.getLogger(DynamicCronjobSchedulerService.class);
    private final CronjobRepository cronjobRepository;
    private final CronjobExecutionRepository mappingRepository;
    private final ExecutionStartService executionStartService;
    private final ThreadPoolTaskScheduler scheduler;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks =
            new ConcurrentHashMap<>();

    public DynamicCronjobSchedulerService(
            CronjobRepository cronjobRepository,
            CronjobExecutionRepository mappingRepository,
            ExecutionStartService executionStartService,
            ThreadPoolTaskScheduler scheduler) {
        this.cronjobRepository = cronjobRepository;
        this.mappingRepository = mappingRepository;
        this.executionStartService = executionStartService;
        this.scheduler = scheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reloadAll() {
        cronjobRepository.findAllHavingEnabledExecutions().forEach(cronjob -> {
            try {
                schedule(cronjob);
            } catch (RuntimeException exception) {
                log.error("Cannot schedule cronjob id={}", cronjob.getId(), exception);
            }
        });
    }

    public synchronized void scheduleIfNecessary(Long cronjobId) {
        if (!mappingRepository.existsByCronjobIdAndStatusTrue(cronjobId)) {
            cancel(cronjobId);
            return;
        }
        if (scheduledTasks.containsKey(cronjobId)) {
            return;
        }
        cronjobRepository.findById(cronjobId).ifPresent(this::schedule);
    }

    public synchronized void reschedule(Long cronjobId) {
        cancel(cronjobId);
        scheduleIfNecessary(cronjobId);
    }

    public synchronized void schedule(Cronjob cronjob) {
        CronExpression.parse(cronjob.getCronValue());
        cancel(cronjob.getId());
        ScheduledFuture<?> future = scheduler.schedule(() -> executeCronjob(cronjob.getId()), new CronTrigger(cronjob.getCronValue()));
        if (future == null) {
            throw new IllegalStateException("Cannot schedule cronjob " + cronjob.getId());
        }
        scheduledTasks.put(cronjob.getId(), future);
        log.info("Scheduled cronjob id={}, cron={}", cronjob.getId(), cronjob.getCronValue());
    }

    public synchronized void cancel(Long cronjobId) {
        ScheduledFuture<?> future = scheduledTasks.remove(cronjobId);
        if (future != null) {
            future.cancel(false);
            log.info("Cancelled cronjob id={}", cronjobId);
        }
    }

    private void executeCronjob(Long cronjobId) {
        List<CronjobExecution> mappings = mappingRepository.findByCronjobIdAndStatusTrue(cronjobId);
        if (mappings.isEmpty()) {
            cancel(cronjobId);
            return;
        }
        UserDetails systemUserDetails = new UserDetails("cronjob:" + cronjobId);
        for (CronjobExecution mapping : mappings) {
            try {
                BaseResponse<?> response = executionStartService.start(mapping.getExecutionInfo().getId(), systemUserDetails);
                log.info("Cronjob id={} started execution id={}, result={}", cronjobId, mapping.getExecutionInfo().getId(), response.getMessage());
            } catch (RuntimeException exception) {
                log.error("Cronjob id={} failed to start execution id={}", cronjobId, mapping.getExecutionInfo().getId(), exception);
            }
        }
    }

    public boolean isScheduled(Long cronjobId) {
        ScheduledFuture<?> future = scheduledTasks.get(cronjobId);
        return future != null && !future.isCancelled() && !future.isDone();
    }

    @PreDestroy
    public void cancelAll() {
        scheduledTasks.values().forEach(future -> future.cancel(false));
        scheduledTasks.clear();
    }
}
