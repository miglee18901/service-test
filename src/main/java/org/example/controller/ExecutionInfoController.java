package org.example.controller;

import org.example.dao.ExecuteVimProperties;
import org.example.dao.UserDetails;
import org.example.model.ExecutionInfo;
import org.example.repository.ExecutionInfoRepository;
import org.example.service.ExecutionInfoService;
import org.example.service.BatchExecutionService;
import org.example.dao.BatchExecutionRequest;
import org.example.dao.BatchExecutionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ExecutionInfoController extends BaseResponseApi implements ExecutionInfoApi {
    private final ExecutionInfoRepository repository;
    private final ExecutionInfoService executionInfoService;
    private final BatchExecutionService batchExecutionService;

    public ExecutionInfoController(ExecutionInfoRepository repository,
                                   ExecutionInfoService executionInfoService,
                                   BatchExecutionService batchExecutionService) {
        this.repository = repository;
        this.executionInfoService = executionInfoService;
        this.batchExecutionService = batchExecutionService;
    }

    @Override
    public ResponseEntity<BatchExecutionResponse> executeBatch(BatchExecutionRequest request, String username) {
        BatchExecutionResponse response = batchExecutionService.executeBatch(request, username);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Override
    public Page<ExecutionInfo> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public BaseResponse start(Long id, String username) {
        return executionInfoService.start(id, new UserDetails(username));
    }

    @Override
    public ResponseEntity<?> listExecuteVim() {
        BaseResponse result = executionInfoService.listExecutionVim();
        return statusResponse(result);
    }
}
