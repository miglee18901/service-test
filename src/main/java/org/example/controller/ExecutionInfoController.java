package org.example.controller;

import org.example.dto.BaseResponse;
import org.example.dto.UserDetails;
import org.example.entity.ExecutionInfo;
import org.example.repository.ExecutionInfoRepository;
import org.example.service.ExecutionStartService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/execution-info")
public class ExecutionInfoController {
    private final ExecutionInfoRepository repository;
    private final ExecutionStartService startService;

    public ExecutionInfoController(ExecutionInfoRepository repository, ExecutionStartService startService) {
        this.repository = repository;
        this.startService = startService;
    }

    @GetMapping
    public Page<ExecutionInfo> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @PostMapping("/{id}/start")
    public BaseResponse<Map<String, String>> start(@PathVariable Long id, @RequestHeader(value = "X-User", defaultValue = "anonymous") String username) {
        return startService.start(id, new UserDetails(username));
    }
}
