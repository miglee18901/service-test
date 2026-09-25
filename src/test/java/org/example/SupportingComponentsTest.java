package org.example;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.example.client.MockExecutionApiClient;
import org.example.config.AsyncConfiguration;
import org.example.config.DataSourceConfig;
import org.example.config.TaskExecutorConfig;
import org.example.dao.StartExecutionRequest;
import org.example.model.ExecutionElement;
import org.example.model.ExecutionInfo;
import org.example.repository.ExecutionInfoRepository;
import org.example.service.ExecutionInfoAction;
import org.example.service.ExecutionInfoHistoryService;
import org.example.service.ExecutionUserLogService;
import org.junit.jupiter.api.Test;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.data.projection.DefaultMethodInvokingMethodInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupportingComponentsTest {
    @Test
    void start_validId_returnsRequestedId() {
        assertEquals("{\"id\":7,\"status\":\"ACCEPTED\"}", new MockExecutionApiClient().start(7L).getBody());
        assertEquals(HttpStatus.OK, new MockExecutionApiClient().start(7L).getStatusCode());
    }

    @Test
    void historyAndLogging_validExecution_processesSuccessfully() {
        ExecutionInfo info = new ExecutionInfo();
        info.setId(1L);
        ExecutionInfoHistoryService history = new ExecutionInfoHistoryService();
        long id = history.checkOldExecutionHistory(info);
        assertEquals(id, history.getMaxExecutionHistory());
        new ExecutionUserLogService().log(info, ExecutionInfoAction.START, "tester");
        assertEquals(ExecutionInfoAction.START, ExecutionInfoAction.valueOf("START"));
        assertTrue(ExecutionInfoAction.values().length > 0);
    }

    @Test
    void configuration_defaultSettings_createsDedicatedExecutors() {
        TaskExecutionProperties properties = new TaskExecutionProperties();
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new AsyncConfiguration(properties).getAsyncExecutor();
        assertEquals(properties.getPool().getCoreSize(), executor.getCorePoolSize());
        assertEquals(properties.getThreadNamePrefix(), executor.getThreadNamePrefix());

        ThreadPoolTaskScheduler scheduler = new TaskExecutorConfig().cronjobTaskScheduler(3);
        assertEquals(3, scheduler.getPoolSize());
        assertEquals("cronjob-", scheduler.getThreadNamePrefix());

        ComboPooledDataSource dataSource = (ComboPooledDataSource) new DataSourceConfig().dataSource();
        assertNotNull(dataSource);
        dataSource.close();
    }

    @Test
    void executionModels_assignedValues_exposesValues() {
        Date now = new Date();
        ExecutionInfo info = new ExecutionInfo();
        info.setId(1L);
        info.setName("name");
        info.setDescription("description");
        info.setState(2);
        info.setExecutionLevel(3);
        info.setUser("user");
        info.setExecutionTime(now);
        info.setDomainId(4);
        info.setExecutionName("execution");
        info.setTimeExecute("10");
        info.setUserExecute("runner");
        info.setCurrentState(5);
        info.setCreateTime(now);
        info.setStartExecutionTime(now);
        info.setCompletedTime(now);
        info.setEnvInfo("env");
        info.setPosIndex(6);
        info.setCategoryId(7L);
        info.setIsNumberTest(false);
        info.setExecutionElements(Collections.emptyList());

        assertEquals(1L, info.getId());
        assertEquals("name", info.getName());
        assertEquals("description", info.getDescription());
        assertEquals(2, info.getState());
        assertEquals(3, info.getExecutionLevel());
        assertEquals("user", info.getUser());
        assertEquals(now, info.getExecutionTime());
        assertEquals(4, info.getDomainId());
        assertEquals("execution", info.getExecutionName());
        assertEquals("10", info.getTimeExecute());
        assertEquals("runner", info.getUserExecute());
        assertEquals(5, info.getCurrentState());
        assertEquals(now, info.getCreateTime());
        assertEquals(now, info.getStartExecutionTime());
        assertEquals(now, info.getCompletedTime());
        assertEquals("env", info.getEnvInfo());
        assertEquals(6, info.getPosIndex());
        assertEquals(7L, info.getCategoryId());
        assertFalse(info.getIsNumberTest());
        assertTrue(info.getExecutionElements().isEmpty());

        ExecutionElement element = new ExecutionElement();
        element.setId(8L);
        element.setName("element");
        element.setExecutionInfo(info);
        assertEquals(8L, element.getId());
        assertEquals("element", element.getName());
        assertEquals(info, element.getExecutionInfo());

        StartExecutionRequest request = new StartExecutionRequest();
        request.setHandleTestCaseMin(5);
        request.setTimeout(100L);
        request.setTimeSleep(10L);
        assertEquals(5, request.getHandleTestCaseMin());
        assertEquals(100L, request.getTimeout());
        assertEquals(10L, request.getTimeSleep());
    }

    @Test
    void findOne_existingExecution_unwrapsOptional() {
        ExecutionInfo info = new ExecutionInfo();
        ProxyFactory factory = new ProxyFactory();
        factory.setInterfaces(ExecutionInfoRepository.class);
        factory.addAdvice((MethodInterceptor) invocation -> {
            if ("findById".equals(invocation.getMethod().getName())) {
                return Long.valueOf(1L).equals(invocation.getArguments()[0])
                        ? Optional.of(info) : Optional.empty();
            }
            return invocation.proceed();
        });
        factory.addAdvice(new DefaultMethodInvokingMethodInterceptor());
        ExecutionInfoRepository repository = (ExecutionInfoRepository) factory.getProxy();

        assertEquals(info, repository.findOne(1L));
        assertNull(repository.findOne(2L));
    }
}
