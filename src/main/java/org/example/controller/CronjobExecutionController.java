package org.example.controller;

import org.example.dto.*;
import org.example.exception.ApiException;
import org.example.service.CronjobExecutionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<BaseResponse<CronjobExecutionResponse>> create(@Valid @RequestBody CronjobExecutionRequest request) {
        CronjobExecutionResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new BaseResponse<>(201, "Created successfully", response));
    }

    @GetMapping("/{id}")
    public BaseResponse<CronjobExecutionResponse> findById(@PathVariable Long id) {
        return new BaseResponse<>(200, "Success", service.findById(id));
    }

    @GetMapping
    public BaseResponse<Page<CronjobExecutionResponse>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long cronjobId,
            @RequestParam(required = false) Long executionInfoId,
            @RequestParam(required = false) Boolean status,
            Pageable pageable) {
        validatePageable(pageable);
        return new BaseResponse<>(200, "Success", service.search(keyword, cronjobId, executionInfoId, status, pageable));
    }

    @PutMapping
    public BaseResponse<CronjobExecutionResponse> update(@Valid @RequestBody CronjobExecutionUpdateRequest request) {
        return new BaseResponse<>(200, "Updated successfully", service.update(request.getId(), request));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return new BaseResponse<>(200, "Deleted successfully", null);
    }

    @PatchMapping("/status")
    public BaseResponse<CronjobExecutionResponse> changeStatus(@Valid @RequestBody ChangeStatusRequest request) {
        return new BaseResponse<>(200, "Status updated successfully", service.changeStatus(request.getId(), request));
    }

    private void validatePageable(Pageable pageable) {
        if (pageable.getPageNumber() < 0
                || pageable.getPageSize() < 1
                || pageable.getPageSize() > 100) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be >= 0 and size must be between 1 and 100");
        }
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORTS.contains(order.getProperty())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported sort property: " + order.getProperty());
            }
        });
    }
}
