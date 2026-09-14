package org.example.controller;

import org.example.dto.*;
import org.springframework.web.server.ResponseStatusException;
import org.example.service.CronjobExecutionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/cronjob-executions")
public class CronjobExecutionController {
    private static final Set<String> ALLOWED_SORTS = new HashSet<>(Arrays.asList("id", "status", "cronjob.id", "executionInfo.id"));
    private final CronjobExecutionService service;

    public CronjobExecutionController(CronjobExecutionService service) {
        this.service = service;
    }

    @PostMapping
    public BaseResponse<CronjobExecutionResponse> create(@Valid @RequestBody CronjobExecutionRequest request) {
        CronjobExecutionResponse response = service.create(request);
        return new BaseResponse<>(HttpStatus.OK.value(), "Created successfully", response);
    }

    @GetMapping("/{id}")
    public BaseResponse<CronjobExecutionResponse> findById(@PathVariable Long id) {
        return new BaseResponse<>(HttpStatus.OK.value(), "Success", service.findById(id));
    }

    @GetMapping
    public BaseResponse<Page<CronjobExecutionResponse>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long cronjobId,
            @RequestParam(required = false) Long executionInfoId,
            @RequestParam(required = false) Boolean status,
            Pageable pageable) {
        validatePageable(pageable);
        return new BaseResponse<>(HttpStatus.OK.value(), "Success", service.search(keyword, cronjobId, executionInfoId, status, pageable));
    }

    @PutMapping("/{id}")
    public BaseResponse<CronjobExecutionResponse> update(@PathVariable Long id, @Valid @RequestBody CronjobExecutionRequest request) {
        return new BaseResponse<>(HttpStatus.OK.value(), "Updated successfully", service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return new BaseResponse<>(HttpStatus.OK.value(), "Deleted successfully", null);
    }

    @PatchMapping("/{id}/status")
    public BaseResponse<CronjobExecutionResponse> changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {
        return new BaseResponse<>(HttpStatus.OK.value(), "Status updated successfully", service.changeStatus(id, request));
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
