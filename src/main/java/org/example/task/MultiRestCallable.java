package org.example.task;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.constants.ServiceConstants;
import org.example.dao.*;
import org.example.service.ThreadExecuteManager;
import org.example.utils.ConnectionManager;
import org.example.utils.HttpClientUtil;
import org.example.utils.OcsToolUtil;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MultiRestCallable implements Callable<ExecuteObj> {
    private static final Logger logger = LogManager.getLogger(MultiRestCallable.class);
    private static final Logger tpsLogger = LogManager.getLogger(TpsLog.class);
    private final String threadName;
    private final OcsVcServerDTO server;
    private final OcsVcCommandDTO command;
    private final List<BatchParameter> listBatchParam;
    private final List<OcsVcParameterDTO> listParameter;
    private final ExecuteObj executeObj;
    private final String accessToken;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition cond = this.lock.newCondition();
    public AtomicLong aCountTotalSent = new AtomicLong(0L);

    private final TpsEntity tpsEntity;

    public MultiRestCallable(String threadName, @NotNull ExecuteObj executeObj) {
        this.threadName = threadName;
        this.executeObj = executeObj;
        this.server = executeObj.getServer();
        this.accessToken = executeObj.getAuthToken();
        this.command = executeObj.getCommand();
        this.listParameter = executeObj.getListParameter();
        this.listBatchParam = executeObj.getListBatchParam();
        this.tpsEntity = executeObj.getTpsEntity();
    }

    @Override
    public ExecuteObj call() throws Exception {
        Integer tpsServer = server.getTps();
        tpsServer = tpsServer != null && tpsServer > 0 ? tpsServer : 100;
        logger.info(threadName + ": Start send " + this.listBatchParam.size() + " requests to " + server.getName() + " server (tps = " + tpsServer + ")");
        this.executeObj.setStartTime(System.currentTimeMillis());

        try {
            int totalMsg = 0;
            int totalError = 0;

            if (this.tpsEntity.getLastCheckTime().get() == 0L) {
                this.tpsEntity.getLastCheckTime().set(System.currentTimeMillis());
            }

            AtomicLong lastCheckTime = this.tpsEntity.getLastCheckTime();
            long startTime = lastCheckTime.get();
            long timeSentOne = 0L;
            AtomicLong aCountSent = this.tpsEntity.getaCountSent();

            int paramSetIndex = 0;
            for (BatchParameter paramSet : listBatchParam) {
                if (paramSet.getParamMap() == null || paramSet.getParamMap().isEmpty()) {
                    continue;
                }
                if (paramSet.getParamMap().size() != listParameter.size()) {
                    logger.error("[{}] paramSetIndex {} not match number of value", threadName, paramSetIndex);
                    continue;
                }
                totalMsg++;

                ExecuteResponse response = new ExecuteResponse();

                try {
                    startTime = System.currentTimeMillis();
                    CommandRequest commandRequest = buildCommandRequest(paramSet);
                    // Mock-only transport context; the HTTP flow below stays the same as the screenshots.
                    ConnectionManager.getInstance().setMockParameter(paramSet);
                    // execute
                    response = executeSingle(commandRequest, server);
                    response.setInputData(getParamSetString(paramSet));

                    //<editor-fold defaultstate="collapsed" desc="tps control">
                    aCountSent.incrementAndGet();
                    aCountTotalSent.incrementAndGet();
                    timeSentOne = System.currentTimeMillis() - startTime;
                    response.setProcessTime(timeSentOne);
                    Double tps = aCountSent.get() * 1000.00 / (System.currentTimeMillis() - lastCheckTime.get());

                    while (tps > tpsServer) {
                        this.lock.lock();
                        try {
                            long sleep = TimeUnit.MILLISECONDS.toNanos(timeSentOne);

                            if (sleep > 0L) {
                                this.cond.awaitNanos(sleep);
                            }
                        } finally {
                            this.lock.unlock();
                        }
                        tps = aCountSent.get() * 1000.00 / (System.currentTimeMillis() - lastCheckTime.get());
                    }
                    long current = System.currentTimeMillis();
                    synchronized (this.tpsEntity) {
                        if (current - lastCheckTime.get() > 5000L) {
                            tps = aCountSent.get() * 1000L / (current * 1.0 - lastCheckTime.get());
                            tpsLogger.info(new TpsLog(aCountSent.get(), tps, aCountTotalSent.get()));
                            lastCheckTime.set(System.currentTimeMillis() - 1000);
                            aCountSent.set(tps.longValue());
                        }
                    }
                    // </editor-fold>

                } catch (Exception e) {
                    logger.error("CANNOT BUILD COMMAND REQUEST");
                    totalError++;
                    totalMsg = 0;
                    logger.error(this.threadName + " execute failure");
                    this.executeObj.setStatus(ServiceConstants.ExecuteStatus.FINISH_FAIL_STATUS);
                } finally {
                    ConnectionManager.getInstance().clearMockParameter();
                    if (response.getStatus() != null && response.getStatus() == ServiceConstants.ExecuteStatus.FINISH_SUCCESS_STATUS) {
                        this.executeObj.getSuccessList().add(response);
                    } else {
                        this.executeObj.getFailList().add(response);
                    }
                    this.executeObj.setTotalMsg(totalMsg);
                    this.executeObj.setTotalError(totalError);
                    ThreadExecuteManager.getInstance().incrementProgress(this.executeObj.getProgressRunId());
                }
                paramSetIndex++;
                logger.info("[{}] response of paramSetIndex {}: {}", threadName, paramSetIndex, response);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            this.executeObj.setStatus(ServiceConstants.ExecuteStatus.FINISH_FAIL_STATUS);
        } finally {
            this.executeObj.setEndTime(System.currentTimeMillis());
            this.executeObj.setFinishStatus(ServiceConstants.BatchFinishStatus.FINISH);
            logger.info(this.threadName + " finished");
        }
        return this.executeObj;
    }

    private @NotNull ExecuteResponse executeSingle(CommandRequest commandRequest, OcsVcServerDTO server) {
        ExecuteResponse response = new ExecuteResponse();
        URI apiUrl = null;
        try {
            apiUrl = HttpClientUtil.getSafeHttpUri(server.getHost(), commandRequest.getPath());
            if (apiUrl == null) {
                response.setStatus(ServiceConstants.ExecuteStatus.NOT_FOUND_SERVER);
                response.setResponse(ServiceConstants.ExecuteDescription.NOT_FOUND_SERVER);
                return response;
            }
            logger.info("[{}] Send OcsTool batch: url={}, method={}, request={}", threadName,
                    apiUrl, commandRequest.getMethod().toUpperCase(), commandRequest.getBody());

            RestTemplate restTemplate = ConnectionManager.getInstance().createRestTemplate(executeObj.getConnectionTimeout(), executeObj.getReadTimeout());
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            if (commandRequest.getHeader() != null && !commandRequest.getHeader().isEmpty()) {
                for (Map.Entry<String, String> headerConfig : commandRequest.getHeader().entrySet()) {
                    headers.add(headerConfig.getKey(), headerConfig.getValue());
                }
            }
            logger.info("headers: {}", headers.toSingleValueMap());
            HttpEntity<String> entity = new HttpEntity<>(commandRequest.getBody(), headers);
            ResponseEntity<?> result;
            try {
                result = ConnectionManager.getInstance().sentHttp(restTemplate, apiUrl,
                        HttpMethod.valueOf(commandRequest.getMethod().toUpperCase()),
                        entity,
                        String.class);
                logger.info("[{}] clientResponse: {}", threadName, result);
                response.setResponse((String) result.getBody());

                if (result.getStatusCode() == HttpStatus.OK) {
                    logger.info("[{}] Execute Success: Response Body: {}", threadName, response.getResponse());
                    if (OcsToolUtil.checkResultCode(response.getResponse(), command.getSuccessCode())) {
                        response.setStatus(ServiceConstants.ExecuteStatus.FINISH_SUCCESS_STATUS);
                    } else {
                        response.setStatus(ServiceConstants.ExecuteStatus.FINISH_FAIL_STATUS);
                    }
                } else {
                    response.setStatus(ServiceConstants.ExecuteStatus.FINISH_FAIL_STATUS);
                }
            } catch (HttpClientErrorException.Unauthorized e) {
                logger.error("[{}] Unauthorized token! Get token retry.", threadName);
                response.setStatus(ServiceConstants.ExecuteStatus.NOT_AUTHEN);
                response.setResponse(e.getMessage());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                logger.error("[{}] Execute Failed: Exception: {}", threadName, e.getMessage());
                response.setStatus(ServiceConstants.ExecuteStatus.FINISH_FAIL_STATUS);
                response.setResponse(ServiceConstants.ExecuteDescription.EXCEPTION_IN_EXECUTE_BATCH);
            }
        } catch (Exception e) {
            response.setStatus(ServiceConstants.ExecuteStatus.FINISH_FAIL_STATUS);
            response.setResponse(ServiceConstants.ExecuteDescription.EXCEPTION_IN_EXECUTE_BATCH);
        }
        return response;
    }

    private CommandRequest buildCommandRequest(@NotNull BatchParameter paramSet) {
        logger.info("[{}] paramSet: {}", threadName, paramSet.getParamMap());
        List<OcsVcParameterDTO> listParamWithValue = new ArrayList<>();
        for (Map.Entry<String, String> entry : paramSet.getParamMap().entrySet()) {
            OcsVcParameterDTO parameterDTO = listParameter.stream()
                    .filter(p -> p.getName().equals(entry.getKey()))
                    .findFirst().orElse(null);
            if (parameterDTO == null) {
                continue;
            }
            OcsVcParameterDTO paramClone = parameterDTO.vClone();
            paramClone.setValueString(entry.getValue());
            listParamWithValue.add(paramClone);
        }
        String commandCode = OcsToolUtil.parseParameter(command.getCode(), listParamWithValue);
        return OcsToolUtil.convertXmlToCommandRequest(commandCode);
    }

    // This helper's body is not shown in the screenshots.
    private String getParamSetString(BatchParameter paramSet) {
        return paramSet.getParamMap().toString();
    }
}
