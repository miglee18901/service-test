package org.example.controller;

import org.example.dao.BaseResponse;
import org.example.dao.BatchChangeStatusRequest;
import org.example.dao.CronjobRequest;
import org.example.dao.CronjobResponse;
import org.example.service.CronjobExecutionService;
import org.example.service.CronjobService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RestController
public class CronjobController implements CronjobApi {
    private static final Set<String> ALLOWED_SORTS = new HashSet<>(Arrays.asList("id", "name", "cronValue"));
    private final CronjobService cronjobService;
    private final CronjobExecutionService executionService;

    public CronjobController(CronjobService cronjobService, CronjobExecutionService executionService) {
        this.cronjobService = cronjobService;
        this.executionService = executionService;
    }

    @Override
    public BaseResponse create(CronjobRequest request) {
        CronjobResponse response = cronjobService.create(request);
        return new BaseResponse(HttpStatus.OK.value(), "Created successfully", response);
    }

    @Override
    public BaseResponse findById(Long id) {
        return new BaseResponse(HttpStatus.OK.value(), "Success", cronjobService.findById(id));
    }

    @Override
    public BaseResponse search(String keyword, Pageable pageable) {
        validatePageable(pageable);
        return new BaseResponse(HttpStatus.OK.value(), "Success", cronjobService.search(keyword, pageable));
    }

    @Override
    public BaseResponse update(Long id, CronjobRequest request) {
        return new BaseResponse(HttpStatus.OK.value(), "Updated successfully", cronjobService.update(id, request));
    }

    @Override
    public BaseResponse delete(Long id) {
        cronjobService.delete(id);
        return new BaseResponse(HttpStatus.OK.value(), "Deleted successfully", null);
    }

    @Override
    public BaseResponse changeAllStatuses(Long cronjobId, BatchChangeStatusRequest request) {
        return new BaseResponse(HttpStatus.OK.value(), "Statuses updated successfully", executionService.changeAllStatuses(cronjobId, request));
    }

    private void validatePageable(Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be >= 0 and size must be between 1 and 100");
        }
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORTS.contains(order.getProperty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort property: " + order.getProperty());
            }
        });
    }
}
