package org.example.controller;

import org.example.dao.BaseResponse;
import org.example.dao.UserDetails;
import org.example.model.ExecutionInfo;
import org.example.repository.ExecutionInfoRepository;
import org.example.service.ExecutionStartService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExecutionInfoController implements ExecutionInfoApi {
    private final ExecutionInfoRepository repository;
    private final ExecutionStartService startService;

    public ExecutionInfoController(ExecutionInfoRepository repository, ExecutionStartService startService) {
        this.repository = repository;
        this.startService = startService;
    }

    @Override
    public Page<ExecutionInfo> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public BaseResponse start(Long id, String username) {
        return startService.start(id, new UserDetails(username));
    }
}
