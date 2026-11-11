package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.client.MockBatchExecutionClient;
import org.example.config.ApplicationProperties;
import org.example.constants.HttpRequestConstants;
import org.example.constants.ServiceConstants;
import org.example.dao.*;
import org.example.task.MultiRestCallable;
import org.example.utils.OcsToolUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BatchExecutionServiceTest {
    @TempDir Path directory;
    private ValidatorFactory validators;
    private ApplicationProperties properties;
    private OcsToolBatchExecutionService ocsService;
    private BatchExecutionService service;

    @BeforeEach
    void setUp() {
        ThreadExecuteManager.shutdown();
        validators = Validation.buildDefaultValidatorFactory();
        properties = new ApplicationProperties();
        properties.getExecuteConfig().setMaxThread(3);
        ocsService = new OcsToolBatchExecutionService(properties, new MockBatchExecutionClient(),
                validators.getValidator(), new ObjectMapper(), directory.toString());
        service = new BatchExecutionService(ocsService);
    }

    @AfterEach
    void tearDown() {
        ocsService.shutdown();
        validators.close();
    }

    @Test
    void executeBatch_usesScreenshotPartitioningAndCleansTrackingMaps() throws Exception {
        BatchExecutionRequest request = request(8);
        request.setNumOfThread(100);
        BatchExecutionResponse response = service.executeBatch(request, "operator");
        assertEquals(200, response.getStatus());
        assertEquals(HttpRequestConstants.ErrorCode.Success, response.getRetn());
        assertEquals(8, response.getTotalSent());
        assertEquals(8, response.getTotalSuccess());
        assertEquals(0, response.getTotalError());
        Set<String> input = response.getListSuccessResponse().stream()
                .map(ExecuteResponse::getInputData).collect(Collectors.toSet());
        assertEquals(8, input.size());
        assertTrue(Files.readAllBytes(Paths.get(response.getResponseLogFile())).length > 0);
        assertTrue(response.getEndTime() >= response.getStartTime());
        assertEquals(response.getEndTime() - response.getStartTime(), response.getProcessTime());
        assertTrackingCleared();
    }

    @Test
    void executeBatch_mixedHttpAndResultFailuresKeepSuccessReturnCode() {
        BatchExecutionRequest request = request(4);
        request.getListBatchParam().get(1).setMockSuccess(false);
        request.getListBatchParam().get(2).setMockHttpStatus(401);
        request.getListBatchParam().get(3).setMockHttpStatus(500);
        BatchExecutionResponse response = service.executeBatch(request, "operator");
        assertEquals(200, response.getStatus());
        assertEquals(HttpRequestConstants.ErrorCode.Success, response.getRetn());
        assertEquals(HttpRequestConstants.Description.BATCH_EXECUTE_NOT_SUCCESS_ALL, response.getDescription());
        assertEquals(4, response.getTotalSent());
        assertEquals(1, response.getTotalSuccess());
        assertEquals(3, response.getTotalError());
        assertTrue(response.getListFailResponse().stream()
                .anyMatch(r -> ServiceConstants.ExecuteStatus.NOT_AUTHEN.equals(r.getStatus())));
        assertTrackingCleared();
    }

    @Test
    void executeBatch_tokenFailureReturnsBeforeRegisteringAndCanRetry() {
        BatchExecutionRequest request = request(1);
        request.getAuthServer().setTokenAvailable(false);
        BatchExecutionResponse response = service.executeBatch(request, "operator");
        assertEquals(401, response.getStatus());
        assertEquals("-2", response.getRetn());
        assertEquals("GET_TOKEN_FAILED", response.getDescription());
        assertEquals(0, response.getStartTime());
        assertTrackingCleared();
        request.getAuthServer().setTokenAvailable(true);
        assertEquals(200, service.executeBatch(request, "operator").getStatus());
    }

    @Test
    void invalidRequests_areRejectedByConditionCheck() {
        assertEquals(400, service.executeBatch(null, "operator").getStatus());
        assertEquals(400, service.executeBatch(request(0), "operator").getStatus());
        BatchExecutionRequest request = request(1);
        request.setNumOfThread(0);
        assertEquals(400, service.executeBatch(request, "operator").getStatus());
        request = request(1);
        request.getListBatchParam().set(0, null);
        assertEquals(400, service.executeBatch(request, "operator").getStatus());
        assertThrows(IllegalArgumentException.class, () -> ocsService.executeBatch(null));
    }

    @Test
    void emptyOrMismatchedParamMaps_areSkippedAsInScreenshots() {
        BatchExecutionRequest request = request(2);
        request.getListBatchParam().get(0).setParamMap(Collections.emptyMap());
        request.getListBatchParam().get(1).getParamMap().put("extra", "value");
        BatchExecutionResponse response = service.executeBatch(request, "operator");
        assertEquals(500, response.getStatus());
        assertEquals(0, response.getTotalSent());
        assertEquals(0, response.getTotalError());
        assertEquals("BATCH_EXECUTE_CANNOT_SENT_REQUEST", response.getDescription());
        assertNull(response.getResponseLogFile());
        assertTrackingCleared();
    }

    @Test
    void worker_buildsCommandClonesParametersAndRecordsTpsAndFinishState() throws Exception {
        BatchExecutionRequest request = request(2);
        ExecuteObj execution = executeObj(request);
        TpsEntity tps = execution.getTpsEntity();
        ExecuteObj completed = new MultiRestCallable("mock_thread_1", execution).call();
        assertSame(execution, completed);
        assertEquals(2, completed.getTotalMsg());
        assertEquals(2, completed.getSuccessList().size());
        assertEquals(2, tps.getaCountSent().get());
        assertTrue(tps.getLastCheckTime().get() > 0);
        assertEquals(ServiceConstants.BatchFinishStatus.FINISH, completed.getFinishStatus());
        assertTrue(completed.getEndTime() >= completed.getStartTime());
        assertNull(request.getCommand().getListParameter().get(0).getValueString());
    }

    @Test
    void executeSingle_handlesInvalidServerAndTransportException() {
        BatchExecutionRequest request = request(1);
        request.getServer().setHost("invalid-host");
        BatchExecutionResponse response = service.executeBatch(request, "operator");
        assertEquals(ServiceConstants.ExecuteStatus.NOT_FOUND_SERVER,
                response.getListFailResponse().get(0).getStatus());
        request = request(1);
        request.getListBatchParam().get(0).setMockException(true);
        response = service.executeBatch(request, "operator");
        assertEquals("EXCEPTION_IN_EXECUTE_BATCH", response.getListFailResponse().get(0).getResponse());
        assertTrackingCleared();
    }

    @Test
    void worker_buildExceptionResetsTotalMsgAsInScreenshot() throws Exception {
        BatchExecutionRequest request = request(1);
        request.getCommand().setCode("<request>");
        ExecuteObj completed = new MultiRestCallable("bad_xml", executeObj(request)).call();
        assertEquals(0, completed.getTotalMsg());
        assertEquals(1, completed.getTotalError());
        assertEquals(1, completed.getFailList().size());
        assertEquals(ServiceConstants.ExecuteStatus.FINISH_FAIL_STATUS, completed.getStatus());
        assertEquals("FINISH", completed.getFinishStatus());
    }

    @Test
    void manager_isSingletonChecksUsernameAndRequiresInitializedResponse() {
        ThreadExecuteManager manager = ThreadExecuteManager.getInstance();
        assertSame(manager, ThreadExecuteManager.getInstance());
        assertNull(manager.addThreads("missing", Collections.emptyList()));
        manager.getMapUserToListExecute().put("operator_10", Collections.emptyList());
        assertTrue(manager.checkUserIsRunningBatch("operator"));
        assertFalse(manager.checkUserIsRunningBatch("another"));
        assertEquals(409, service.executeBatch(request(1), "operator").getStatus());
        manager.getMapUserToListExecute().clear();
    }

    @Test
    void writeLogFailure_setsFailAndStillClearsMaps() throws Exception {
        Path file = Files.createTempFile(directory, "file", ".txt");
        OcsToolBatchExecutionService failing = new OcsToolBatchExecutionService(properties,
                new MockBatchExecutionClient(), validators.getValidator(), new ObjectMapper(), file.toString());
        BatchExecutionResponse response = new BatchExecutionService(failing).executeBatch(request(1), "operator");
        assertEquals(500, response.getStatus());
        assertEquals("-1", response.getRetn());
        assertTrackingCleared();
    }

    @Test
    void xmlHelper_substitutesEscapedValuesAndHeaders() {
        OcsVcParameterDTO parameter = new OcsVcParameterDTO();
        parameter.setName("msisdn");
        parameter.setValueString("a&b<c>");
        String xml = "<request><path>/mock</path><method>POST</method><body>${msisdn}</body>"
                + "<header name=\"X-Mock\">yes</header></request>";
        CommandRequest command = OcsToolUtil.convertXmlToCommandRequest(
                OcsToolUtil.parseParameter(xml, Collections.singletonList(parameter)));
        assertEquals("a&b<c>", command.getBody());
        assertEquals("yes", command.getHeader().get("X-Mock"));
        assertTrue(OcsToolUtil.checkResultCode("{\"code\":\"SUCCESS\"}", "SUCCESS"));
        assertFalse(OcsToolUtil.checkResultCode("{\"code\":\"FAIL\"}", "SUCCESS"));
    }

    private void assertTrackingCleared() {
        ThreadExecuteManager manager = ThreadExecuteManager.getInstance();
        assertTrue(manager.getMapExecuteKeyToResponse().isEmpty());
        assertTrue(manager.getMapServerParamSet().isEmpty());
        assertTrue(manager.getMapUserToListExecute().isEmpty());
    }

    private ExecuteObj executeObj(BatchExecutionRequest request) {
        ExecuteObj execution = new ExecuteObj();
        execution.setExecuteKey("operator_10");
        execution.setServer(request.getServer());
        execution.setCommand(request.getCommand());
        execution.setListParameter(request.getCommand().getListParameter());
        execution.setListBatchParam(request.getListBatchParam());
        execution.setAuthToken("mock-access-token");
        execution.setTpsEntity(new TpsEntity());
        execution.setConnectionTimeout(300000);
        execution.setReadTimeout(300000);
        return execution;
    }

    private BatchExecutionRequest request(int size) {
        BatchExecutionRequest request = new BatchExecutionRequest();
        OcsVcServerDTO server = new OcsVcServerDTO();
        server.setId(1L);
        server.setName("Mock OCS");
        server.setHost("http://mock-ocs");
        server.setTps(100000);
        request.setServer(server);
        OcsVcCommandDTO command = new OcsVcCommandDTO();
        command.setId(10L);
        command.setName("Mock command");
        command.setCode("<request><path>/mock-command</path><method>POST</method><body>${msisdn}</body></request>");
        OcsVcParameterDTO parameter = new OcsVcParameterDTO();
        parameter.setName("msisdn");
        command.setListParameter(Collections.singletonList(parameter));
        request.setCommand(command);
        request.setAuthServer(new BatchExecutionRequest.AuthServer());
        request.setNumOfThread(3);
        for (int i = 0; i < size; i++) {
            BatchParameter batch = new BatchParameter();
            batch.getParamMap().put("msisdn", "1000" + i);
            request.getListBatchParam().add(batch);
        }
        return request;
    }
}
