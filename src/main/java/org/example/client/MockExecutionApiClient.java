package org.example.client;

import org.springframework.stereotype.Component;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Component
public class MockExecutionApiClient {

    public ResponseEntity<String> start(Long executionInfoId) {
        char quote = 34;
        String body = "{" + quote + "id" + quote + ":" + executionInfoId
                + "," + quote + "status" + quote + ":"
                + quote + "ACCEPTED" + quote + "}";
        return new ResponseEntity<>(body, HttpStatus.OK);
    }
}
