package org.example.service;

import org.example.dto.CronjobRequest;
import org.example.dto.CronjobResponse;
import org.example.entity.Cronjob;
import org.example.repository.CronjobExecutionRepository;
import org.example.repository.CronjobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class CronjobService {
    private final CronjobRepository repository;
    private final CronjobExecutionRepository mappingRepository;
    private final DynamicCronjobSchedulerService schedulerService;

    public CronjobService(
            CronjobRepository repository,
            CronjobExecutionRepository mappingRepository,
            DynamicCronjobSchedulerService schedulerService) {
        this.repository = repository;
        this.mappingRepository = mappingRepository;
        this.schedulerService = schedulerService;
    }

    @Transactional
    public CronjobResponse create(CronjobRequest request) {
        String name = normalizeName(request.getName());
        validateCron(request.getCronValue());
        if (repository.existsByNameIgnoreCase(name)) {
            throw conflict("Cronjob name already exists");
        }
        Cronjob cronjob = new Cronjob();
        cronjob.setName(name);
        cronjob.setCronValue(request.getCronValue().trim());
        return CronjobResponse.from(repository.save(cronjob));
    }

    @Transactional(readOnly = true)
    public CronjobResponse findById(Long id) {
        return CronjobResponse.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<CronjobResponse> search(String keyword, Pageable pageable) {
        return repository.search(keyword == null ? "" : keyword.trim(), pageable).map(CronjobResponse::from);
    }

    @Transactional
    public CronjobResponse update(Long id, CronjobRequest request) {
        Cronjob cronjob = getEntity(id);
        String name = normalizeName(request.getName());
        validateCron(request.getCronValue());
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw conflict("Cronjob name already exists");
        }
        boolean cronChanged = !cronjob.getCronValue().equals(request.getCronValue().trim());
        cronjob.setName(name);
        cronjob.setCronValue(request.getCronValue().trim());
        Cronjob saved = repository.save(cronjob);
        if (cronChanged) {
            afterCommit(() -> schedulerService.reschedule(id));
        }
        return CronjobResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Cronjob cronjob = getEntity(id);
        if (mappingRepository.existsByCronjobId(id)) {
            throw conflict("Remove all cronjob executions before deleting cronjob");
        }
        repository.delete(cronjob);
        afterCommit(() -> schedulerService.cancel(id));
    }

    public Cronjob getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cronjob not found"));
    }

    public static void validateCron(String cronValue) {
        if (cronValue == null || cronValue.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cron expression is required");
        }

        String normalizedCron = cronValue.trim();
        if (normalizedCron.split("\\s+").length != 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cron expression must contain exactly 6 fields: second minute hour day-of-month month day-of-week");
        }

        try {
            CronExpression.parse(normalizedCron);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cron expression");
        }
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cronjob name is required");
        }
        return normalized;
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    static void afterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isActualTransactionActive() && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            runnable.run();
                        }
                    });
        } else {
            runnable.run();
        }
    }

}
