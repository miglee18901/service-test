package org.example.controller;

import org.example.dto.BaseResponse;
import org.example.dto.UserDetails;
import org.example.entity.ExecutionInfo;
import org.example.repository.ExecutionInfoRepository;
import org.example.service.ExecutionStartService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
    public BaseResponse<Map<String, String>> start(Long id, String username) {
        return startService.start(id, new UserDetails(username));
    }
}
