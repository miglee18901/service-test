package org.example.client;

import org.example.dao.BatchExecutionRequest;
import org.example.dao.BatchParameter;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Component
public class MockBatchExecutionClient {
    public boolean checkToken(BatchExecutionRequest.AuthServer authServer) {
        return authServer.isTokenAvailable();
    }

    public ResponseEntity<String> execute(URI uri, HttpMethod method, HttpEntity<String> entity, BatchParameter parameter) {
        if (parameter == null) throw new IllegalStateException("Missing mock parameter");
        if (parameter.isMockException()) throw new IllegalStateException("Mock transport exception");
        if (parameter.getMockHttpStatus() >= 400 && parameter.getMockHttpStatus() < 500) {
            throw HttpClientErrorException.create(HttpStatus.valueOf(parameter.getMockHttpStatus()),
                    "Mock HTTP error", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        }
        return ResponseEntity.status(parameter.getMockHttpStatus())
                .body(parameter.isMockSuccess() ? "{\"code\":\"SUCCESS\"}" : "{\"code\":\"FAIL\"}");
    }
}
