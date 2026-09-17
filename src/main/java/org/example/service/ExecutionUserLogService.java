package org.example.service;

import org.example.model.ExecutionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExecutionUserLogService {
    private static final Logger log = LoggerFactory.getLogger(ExecutionUserLogService.class);

    public void log(ExecutionInfo executionInfo, ExecutionInfoAction action, String username) {
        log.info("{} executionInfoId={}, username={}", action, executionInfo.getId(), username);
    }
}
