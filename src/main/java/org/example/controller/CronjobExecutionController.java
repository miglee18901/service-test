package org.example.controller;

import org.example.dto.BaseResponse;
import org.example.dto.ChangeStatusRequest;
import org.example.dto.CronjobExecutionRequest;
import org.example.dto.CronjobExecutionResponse;
import org.example.service.CronjobExecutionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RestController
public class CronjobExecutionController implements CronjobExecutionApi {
    private static final Set<String> ALLOWED_SORTS = new HashSet<>(Arrays.asList("id", "status", "cronjob.id", "executionInfo.id"));
    private final CronjobExecutionService service;

    public CronjobExecutionController(CronjobExecutionService service) {
        this.service = service;
    }

    @Override
    public BaseResponse<CronjobExecutionResponse> create(CronjobExecutionRequest request) {
        CronjobExecutionResponse response = service.create(request);
        return new BaseResponse<>(HttpStatus.OK.value(), "Created successfully", response);
    }

    @Override
    public BaseResponse<CronjobExecutionResponse> findById(Long id) {
        return new BaseResponse<>(HttpStatus.OK.value(), "Success", service.findById(id));
    }

    @Override
    public BaseResponse<Page<CronjobExecutionResponse>> search(
            String keyword,
            Long cronjobId,
            Long executionInfoId,
            Boolean status,
            Pageable pageable) {
        validatePageable(pageable);
        return new BaseResponse<>(HttpStatus.OK.value(), "Success", service.search(keyword, cronjobId, executionInfoId, status, pageable));
    }

    @Override
    public BaseResponse<CronjobExecutionResponse> update(Long id, CronjobExecutionRequest request) {
        return new BaseResponse<>(HttpStatus.OK.value(), "Updated successfully", service.update(id, request));
    }

    @Override
    public BaseResponse<Void> delete(Long id) {
        service.delete(id);
        return new BaseResponse<>(HttpStatus.OK.value(), "Deleted successfully", null);
    }

    @Override
    public BaseResponse<CronjobExecutionResponse> changeStatus(Long id, ChangeStatusRequest request) {
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
