package org.example.service;

import org.example.dao.*;
import org.example.model.Cronjob;
import org.example.model.CronjobExecution;
import org.example.model.ExecutionInfo;
import org.springframework.web.server.ResponseStatusException;
import org.example.repository.CronjobExecutionRepository;
import org.example.repository.ExecutionInfoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CronjobExecutionService {
    private static final String STALE_MESSAGE = "Data has been changed by another session. Please reload the page.";

    private final CronjobExecutionRepository repository;
    private final CronjobService cronjobService;
    private final ExecutionInfoRepository executionInfoRepository;
    private final DynamicCronjobSchedulerService schedulerService;

    public CronjobExecutionService(
            CronjobExecutionRepository repository,
            CronjobService cronjobService,
            ExecutionInfoRepository executionInfoRepository,
            DynamicCronjobSchedulerService schedulerService) {
        this.repository = repository;
        this.cronjobService = cronjobService;
        this.executionInfoRepository = executionInfoRepository;
        this.schedulerService = schedulerService;
    }

    @Transactional
    public CronjobExecutionResponse create(CronjobExecutionRequest request) {
        Cronjob cronjob = cronjobService.getEntity(request.getCronjobId());
        ExecutionInfo execution = getExecution(request.getExecutionInfoId());
        validateExecutionElements(execution);
        if (repository.existsByExecutionInfoId(execution.getId())) {
            throw conflict("Execution already belongs to another cronjob");
        }
        CronjobExecution mapping = new CronjobExecution();
        mapping.setCronjob(cronjob);
        mapping.setExecutionInfo(execution);
        mapping.setStatus(request.getStatus());
        CronjobExecution saved = repository.saveAndFlush(mapping);
        CronjobService.afterCommit(() -> schedulerService.scheduleIfNecessary(cronjob.getId()));
        return CronjobExecutionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public CronjobExecutionResponse findById(Long id) {
        return CronjobExecutionResponse.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<CronjobExecutionResponse> search(String keyword, Long cronjobId, Long executionInfoId, Boolean status, Pageable pageable) {
        return repository.search(keyword == null ? "" : keyword.trim(), cronjobId, executionInfoId, status, pageable).map(CronjobExecutionResponse::from);
    }

    @Transactional
    public CronjobExecutionResponse update(Long id, CronjobExecutionRequest request) {
        CronjobExecution mapping = getEntity(id);
        Long oldCronjobId = mapping.getCronjob().getId();
        Cronjob cronjob = cronjobService.getEntity(request.getCronjobId());
        ExecutionInfo execution = getExecution(request.getExecutionInfoId());
        validateExecutionElements(execution);
        if (repository.existsByExecutionInfoIdAndIdNot(execution.getId(), id)) {
            throw conflict("Execution already belongs to another cronjob");
        }
        mapping.setCronjob(cronjob);
        mapping.setExecutionInfo(execution);
        mapping.setStatus(request.getStatus());
        CronjobExecution saved = repository.saveAndFlush(mapping);
        CronjobService.afterCommit(() -> {
            schedulerService.scheduleIfNecessary(oldCronjobId);
            schedulerService.scheduleIfNecessary(cronjob.getId());
        });
        return CronjobExecutionResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        CronjobExecution mapping = getEntity(id);
        Long cronjobId = mapping.getCronjob().getId();
        repository.delete(mapping);
        repository.flush();
        CronjobService.afterCommit(() -> schedulerService.scheduleIfNecessary(cronjobId));
    }

    @Transactional
    public CronjobExecutionResponse changeStatus(Long id, ChangeStatusRequest request) {
        CronjobExecution mapping = getEntity(id);
        Long cronjobId = mapping.getCronjob().getId();
        int updated = repository.updateStatusIfMatches(id, request.getExpectedStatus(), request.getStatus());
        if (updated == 0) {
            throw conflict(STALE_MESSAGE);
        }
        CronjobService.afterCommit(() -> schedulerService.scheduleIfNecessary(cronjobId));
        CronjobExecution refreshed = getEntity(id);
        return CronjobExecutionResponse.from(refreshed);
    }

    @Transactional
    public List<CronjobExecutionResponse> changeAllStatuses(Long cronjobId, BatchChangeStatusRequest request) {
        cronjobService.getEntity(cronjobId);
        List<CronjobExecution> mappings = repository.findByCronjobId(cronjobId);

        Set<Long> requestIds = request.getItems().stream().map(BatchStatusItem::getId).collect(Collectors.toSet());
        if (requestIds.size() != request.getItems().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate mapping id");
        }
        Set<Long> databaseIds = mappings.stream().map(CronjobExecution::getId).collect(Collectors.toSet());
        if (!databaseIds.equals(requestIds)) {
            throw conflict(STALE_MESSAGE);
        }

        Map<Long, BatchStatusItem> itemById = request.getItems().stream().collect(Collectors.toMap(BatchStatusItem::getId, Function.identity()));
        for (CronjobExecution mapping : mappings) {
            BatchStatusItem item = itemById.get(mapping.getId());
            if (!Objects.equals(mapping.getStatus(), item.getExpectedStatus())) {
                throw conflict(STALE_MESSAGE);
            }
            int updated = repository.updateStatusIfMatches(mapping.getId(), item.getExpectedStatus(), request.getStatus());
            if (updated == 0) {
                throw conflict(STALE_MESSAGE);
            }
        }
        repository.flush();
        CronjobService.afterCommit(() -> schedulerService.scheduleIfNecessary(cronjobId));
        return repository.findByCronjobId(cronjobId).stream().map(CronjobExecutionResponse::from).collect(Collectors.toList());
    }

    private CronjobExecution getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cronjob execution not found"));
    }

    private ExecutionInfo getExecution(Long id) {
        ExecutionInfo execution = executionInfoRepository.findOne(id);
        if (execution == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Execution info not found");
        }
        return execution;
    }

    private void validateExecutionElements(ExecutionInfo execution) {
        if (execution.getExecutionElements() == null || execution.getExecutionElements().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Execution must have at least one execution element");
        }
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
