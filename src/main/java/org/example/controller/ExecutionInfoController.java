package org.example.controller;

import org.example.dao.BaseResponse;
import org.example.dao.UserDetails;
import org.example.model.ExecutionInfo;
import org.example.repository.ExecutionInfoRepository;
import org.example.service.ExecutionInfoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExecutionInfoController implements ExecutionInfoApi {
    private final ExecutionInfoRepository repository;
    private final ExecutionInfoService executionInfoService;

    public ExecutionInfoController(ExecutionInfoRepository repository, ExecutionInfoService executionInfoService) {
        this.repository = repository;
        this.executionInfoService = executionInfoService;
    }

    @Override
    public Page<ExecutionInfo> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public BaseResponse start(Long id, String username) {
        return executionInfoService.start(id, new UserDetails(username));
    }
}
