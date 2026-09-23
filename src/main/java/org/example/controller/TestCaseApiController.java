package org.example.controller;

import org.example.service.TestCaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestCaseApiController extends BaseResponseApi implements TestCaseApi {
    private static final Logger logger = LoggerFactory.getLogger(TestCaseApiController.class);
    private final TestCaseService testCaseService;

    public TestCaseApiController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @Override
    public ResponseEntity<?> getBlockedValueCharacteristics() throws Exception {
        try {
            BaseResponse result = testCaseService.getBlockedValueCharacteristics();
            return statusResponse(result);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            BaseResponse error = new BaseResponse(500, e.getMessage(), null);
            return statusResponse(error);
        }
    }
}
