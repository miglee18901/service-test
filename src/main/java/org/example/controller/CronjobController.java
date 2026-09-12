package org.example.controller;

import org.example.dto.*;
import org.example.exception.ApiException;
import org.example.service.CronjobExecutionService;
import org.example.service.CronjobService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/cronjobs")
public class CronjobController {
    private static final Set<String> ALLOWED_SORTS = new HashSet<>(Arrays.asList("id", "name", "cronValue"));
    private final CronjobService cronjobService;
    private final CronjobExecutionService executionService;

    public CronjobController(CronjobService cronjobService, CronjobExecutionService executionService) {
        this.cronjobService = cronjobService;
        this.executionService = executionService;
    }

    @PostMapping
    public ResponseEntity<BaseResponse<CronjobResponse>> create(@Valid @RequestBody CronjobRequest request) {
        CronjobResponse response = cronjobService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new BaseResponse<>(201, "Created successfully", response));
    }

    @GetMapping("/{id}")
    public BaseResponse<CronjobResponse> findById(@PathVariable Long id) {
        return new BaseResponse<>(200, "Success", cronjobService.findById(id));
    }

    @GetMapping
    public BaseResponse<Page<CronjobResponse>> search(@RequestParam(defaultValue = "") String keyword, Pageable pageable) {
        validatePageable(pageable);
        return new BaseResponse<>(200, "Success", cronjobService.search(keyword, pageable));
    }

    @PutMapping
    public BaseResponse<CronjobResponse> update(@Valid @RequestBody CronjobUpdateRequest request) {
        return new BaseResponse<>(200, "Updated successfully", cronjobService.update(request.getId(), request));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        cronjobService.delete(id);
        return new BaseResponse<>(200, "Deleted successfully", null);
    }

    @PatchMapping("/executions/status")
    public BaseResponse<List<CronjobExecutionResponse>> changeAllStatuses(@Valid @RequestBody BatchChangeStatusRequest request) {
        return new BaseResponse<>(200, "Statuses updated successfully", executionService.changeAllStatuses(request.getCronjobId(), request));
    }

    private void validatePageable(Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Page must be >= 0 and size must be between 1 and 100");
        }
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORTS.contains(order.getProperty())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported sort property: " + order.getProperty());
            }
        });
    }
}
