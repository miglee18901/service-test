package org.example.controller;

import org.example.dto.*;
import org.springframework.web.server.ResponseStatusException;
import org.example.service.CronjobExecutionService;
import org.example.service.CronjobService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
    public BaseResponse<CronjobResponse> create(@Valid @RequestBody CronjobRequest request) {
        CronjobResponse response = cronjobService.create(request);
        return new BaseResponse<>(HttpStatus.OK.value(), "Created successfully", response);
    }

    @GetMapping("/{id}")
    public BaseResponse<CronjobResponse> findById(@PathVariable Long id) {
        return new BaseResponse<>(HttpStatus.OK.value(), "Success", cronjobService.findById(id));
    }

    @GetMapping
    public BaseResponse<Page<CronjobResponse>> search(@RequestParam(defaultValue = "") String keyword, Pageable pageable) {
        validatePageable(pageable);
        return new BaseResponse<>(HttpStatus.OK.value(), "Success", cronjobService.search(keyword, pageable));
    }

    @PutMapping("/{id}")
    public BaseResponse<CronjobResponse> update(@PathVariable Long id, @Valid @RequestBody CronjobRequest request) {
        return new BaseResponse<>(HttpStatus.OK.value(), "Updated successfully", cronjobService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        cronjobService.delete(id);
        return new BaseResponse<>(HttpStatus.OK.value(), "Deleted successfully", null);
    }

    @PatchMapping("/{cronjobId}/executions/status")
    public BaseResponse<List<CronjobExecutionResponse>> changeAllStatuses(@PathVariable Long cronjobId, @Valid @RequestBody BatchChangeStatusRequest request) {
        return new BaseResponse<>(HttpStatus.OK.value(), "Statuses updated successfully", executionService.changeAllStatuses(cronjobId, request));
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
