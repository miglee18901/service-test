package org.example.utils;

import org.example.client.MockBatchExecutionClient;
import org.example.dao.BatchParameter;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.net.URI;

/** Mock transport retaining the signatures used in the supplied executeSingle method. */
public final class ConnectionManager {
    private static final ConnectionManager INSTANCE = new ConnectionManager();
    private final MockBatchExecutionClient client = new MockBatchExecutionClient();
    private final ThreadLocal<BatchParameter> mockParameter = new ThreadLocal<>();

    private ConnectionManager() { }

    public static ConnectionManager getInstance() { return INSTANCE; }

    public void setMockParameter(BatchParameter parameter) { mockParameter.set(parameter); }

    public void clearMockParameter() { mockParameter.remove(); }

    public RestTemplate createRestTemplate(int connectionTimeout, int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }

    public ResponseEntity<String> sentHttp(RestTemplate restTemplate, URI apiUrl,
            HttpMethod method, HttpEntity<String> entity, Class<String> responseType) {
        // Deliberately never call restTemplate.exchange: this project uses mock OCS.
        return client.execute(apiUrl, method, entity, mockParameter.get());
    }
}
