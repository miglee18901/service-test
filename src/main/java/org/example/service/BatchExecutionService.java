package org.example.service;

import org.example.constants.HttpRequestConstants;
import org.example.constants.ServiceConstants;
import org.example.dao.BatchExecutionRequest;
import org.example.dao.BatchExecutionResponse;
import org.example.dao.BatchProgressResponse;
import org.springframework.stereotype.Service;

/** HTTP adapter; the screenshot implementation lives in OcsToolBatchExecutionService. */
@Service
public class BatchExecutionService {
    private final OcsToolBatchExecutionService service;
    private final OcsToolSingleExecutionService singleExecutionService;

    @org.springframework.beans.factory.annotation.Autowired
    public BatchExecutionService(OcsToolBatchExecutionService service, OcsToolSingleExecutionService singleExecutionService) {
        this.service = service;
        this.singleExecutionService = singleExecutionService;
    }

    public BatchExecutionService(OcsToolBatchExecutionService service) {
        this.service = service;
        this.singleExecutionService = null;
    }

    public BatchExecutionResponse executeBatch(BatchExecutionRequest request, String username) {
        try {
            if (request != null) request.setUsername(username);
            BatchExecutionResponse response = service.executeBatch(request);
            response.setStatus(String.valueOf(ServiceConstants.ExecuteStatus.NOT_AUTHEN).equals(response.getRetn())
                    ? 401 : HttpRequestConstants.ErrorCode.Success.equals(response.getRetn()) ? 200 : 500);
            return response;
        } catch (IllegalArgumentException exception) {
            return failure(400, exception.getMessage());
        } catch (IllegalStateException exception) {
            return failure(409, exception.getMessage());
        }
    }

    public BatchProgressResponse<?> getProgress(String executeKey) {
        return service.getProgress(executeKey);
    }

    public org.example.dao.SingleExecutionResponse executeSingle(org.example.dao.SingleExecutionRequest request, String username) {
        return singleExecutionService.executeSingle(request, username);
    }

    private BatchExecutionResponse failure(int status, String message) {
        BatchExecutionResponse response = new BatchExecutionResponse();
        response.setStatus(status);
        response.setRetn(HttpRequestConstants.ErrorCode.Fail);
        response.setDescription(message);
        return response;
    }
}
