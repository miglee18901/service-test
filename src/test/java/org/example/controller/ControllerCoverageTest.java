package org.example.controller;

import org.example.dao.*;
import org.example.model.ExecutionInfo;
import org.example.repository.ExecutionInfoRepository;
import org.example.service.CronjobExecutionService;
import org.example.service.CronjobService;
import org.example.service.ExecutionInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ControllerCoverageTest {
    @Test
    void cronjobController_validRequests_delegatesOperations() {
        CronjobService cronjobs = mock(CronjobService.class);
        CronjobExecutionService executions = mock(CronjobExecutionService.class);
        CronjobController controller = new CronjobController(cronjobs, executions);
        CronjobRequest request = new CronjobRequest();
        CronjobResponse response = new CronjobResponse(1L, "job", "0 * * * * *");
        Page<CronjobResponse> page = new PageImpl<>(Collections.singletonList(response));
        when(cronjobs.create(request)).thenReturn(response);
        when(cronjobs.findById(1L)).thenReturn(response);
        when(cronjobs.search(eq("job"), any())).thenReturn(page);
        when(cronjobs.update(1L, request)).thenReturn(response);
        when(executions.changeAllStatuses(eq(1L), any())).thenReturn(Collections.emptyList());

        assertEquals(response, controller.create(request).getData());
        assertEquals(response, controller.findById(1L).getData());
        assertEquals(page, controller.search("job", PageRequest.of(0, 10, Sort.by("name"))).getData());
        assertEquals(response, controller.update(1L, request).getData());
        assertEquals(HttpStatus.OK.value(), controller.delete(1L).getStatus());
        assertNotNull(controller.changeAllStatuses(1L, new BatchChangeStatusRequest()).getData());
        verify(cronjobs).delete(1L);
    }

    @Test
    void cronjobController_invalidPagingOrSort_rejectsRequest() {
        CronjobController controller = new CronjobController(mock(CronjobService.class), mock(CronjobExecutionService.class));
        assertThrows(ResponseStatusException.class,
                () -> controller.search("", PageRequest.of(0, 101)));
        assertThrows(ResponseStatusException.class,
                () -> controller.search("", PageRequest.of(0, 10, Sort.by("invalid"))));
    }

    @Test
    void executionController_validRequests_delegatesOperations() {
        CronjobExecutionService service = mock(CronjobExecutionService.class);
        CronjobExecutionController controller = new CronjobExecutionController(service);
        CronjobExecutionRequest request = new CronjobExecutionRequest();
        CronjobExecutionResponse response = mock(CronjobExecutionResponse.class);
        Page<CronjobExecutionResponse> page = new PageImpl<>(Collections.singletonList(response));
        when(service.create(request)).thenReturn(response);
        when(service.findById(1L)).thenReturn(response);
        when(service.search(eq("key"), eq(2L), eq(3L), eq(true), any())).thenReturn(page);
        when(service.update(1L, request)).thenReturn(response);
        when(service.changeStatus(eq(1L), any())).thenReturn(response);

        assertEquals(response, controller.create(request).getData());
        assertEquals(response, controller.findById(1L).getData());
        assertEquals(page, controller.search("key", 2L, 3L, true,
                PageRequest.of(0, 10, Sort.by("status"))).getData());
        assertEquals(response, controller.update(1L, request).getData());
        assertEquals(HttpStatus.OK.value(), controller.delete(1L).getStatus());
        assertEquals(response, controller.changeStatus(1L, new ChangeStatusRequest()).getData());
        verify(service).delete(1L);
    }

    @Test
    void executionController_invalidPagingOrSort_rejectsRequest() {
        CronjobExecutionController controller = new CronjobExecutionController(mock(CronjobExecutionService.class));
        assertThrows(ResponseStatusException.class,
                () -> controller.search("", null, null, null, PageRequest.of(0, 101)));
        assertThrows(ResponseStatusException.class,
                () -> controller.search("", null, null, null, PageRequest.of(0, 10, Sort.by("invalid"))));
    }

    @Test
    void executionInfoController_validRequests_delegatesRequests() {
        ExecutionInfoRepository repository = mock(ExecutionInfoRepository.class);
        ExecutionInfoService startService = mock(ExecutionInfoService.class);
        ExecutionInfoController controller = new ExecutionInfoController(repository, startService);
        Page<ExecutionInfo> page = Page.empty();
        BaseResponse started = new BaseResponse(200, "ok", Collections.emptyMap());
        when(repository.findAll(any(PageRequest.class))).thenReturn(page);
        when(startService.start(eq(1L), any(UserDetails.class))).thenReturn(started);

        assertEquals(page, controller.findAll(PageRequest.of(0, 10)));
        assertEquals(started, controller.start(1L, "tester"));
        verify(startService).start(eq(1L), argThat(user -> "tester".equals(user.getUsername())));
    }
}