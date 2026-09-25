package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dao.*;
import org.example.service.CronjobExecutionService;
import org.example.service.CronjobService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CronjobControllerTest {
    @Mock
    private CronjobService cronjobService;
    @Mock
    private CronjobExecutionService executionService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CronjobController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        controller = new CronjobController(cronjobService, executionService);
    }

    @Test
    void create_validRequest_returnsCreatedResponse() {
        CronjobRequest request = request();
        CronjobResponse expected = response();
        when(cronjobService.create(request)).thenReturn(expected);

        BaseResponse result = controller.create(request);

        assertEquals(HttpStatus.OK.value(), result.getStatus());
        assertEquals("Created successfully", result.getMessage());
        assertEquals(expected, result.getData());
    }

    @Test
    void findById_existingCronjob_returnsResponse() {
        CronjobResponse expected = response();
        when(cronjobService.findById(9L)).thenReturn(expected);

        BaseResponse result = controller.findById(9L);

        assertEquals("Success", result.getMessage());
        assertEquals(expected, result.getData());
    }

    @Test
    void search_validPageable_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));
        Page<CronjobResponse> expected = new PageImpl<>(Collections.singletonList(response()));
        when(cronjobService.search("daily", pageable)).thenReturn(expected);

        BaseResponse result = controller.search("daily", pageable);

        assertEquals(expected, result.getData());
        verify(cronjobService).search("daily", pageable);
    }

    @Test
    void delete_existingCronjob_returnsSuccess() {
        BaseResponse result = controller.delete(9L);

        assertEquals(HttpStatus.OK.value(), result.getStatus());
        assertEquals("Deleted successfully", result.getMessage());
        assertEquals(null, result.getData());
        verify(cronjobService).delete(9L);
    }

    @Test
    void update_cronjobIdInPath_passesIdToService() throws Exception {
        CronjobRequest request = new CronjobRequest();
        request.setName("Daily");
        request.setCronValue("0 */5 * * * *");
        when(cronjobService.update(eq(9L), any()))
                .thenReturn(new CronjobResponse(
                        9L, "Daily", "0 */5 * * * *"));

        mockMvc().perform(put("/cronjobs/{id}", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(9));

        verify(cronjobService).update(eq(9L), any());
    }

    @Test
    void batchStatus_cronjobIdInPath_passesIdToService() throws Exception {
        BatchStatusItem item = new BatchStatusItem();
        item.setId(11L);
        item.setExpectedStatus(true);
        BatchChangeStatusRequest request = new BatchChangeStatusRequest();
        request.setItems(Collections.singletonList(item));
        request.setStatus(false);
        when(executionService.changeAllStatuses(eq(7L), any()))
                .thenReturn(Collections.emptyList());

        mockMvc().perform(patch("/cronjobs/{cronjobId}/executions/status", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(executionService).changeAllStatuses(eq(7L), any());
    }

    @Test
    void search_negativePage_rejectsRequest() {
        Pageable pageable = mock(Pageable.class);
        when(pageable.getPageNumber()).thenReturn(-1);
        CronjobController controller = new CronjobController(cronjobService, executionService);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.search("", pageable));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Page must be >= 0 and size must be between 1 and 100", exception.getReason());
    }

    @Test
    void search_zeroPageSize_rejectsRequest() {
        Pageable pageable = mock(Pageable.class);
        when(pageable.getPageNumber()).thenReturn(0);
        when(pageable.getPageSize()).thenReturn(0);
        CronjobController controller = new CronjobController(cronjobService, executionService);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.search("", pageable));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Page must be >= 0 and size must be between 1 and 100", exception.getReason());
    }

    @Test
    void search_pageSizeOverMaximum_rejectsRequest() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.search("", PageRequest.of(0, 101)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void search_unsupportedSort_rejectsRequest() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.search("", PageRequest.of(0, 10, Sort.by("unsupported"))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Unsupported sort property: unsupported", exception.getReason());
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(
                controller).build();
    }

    private CronjobRequest request() {
        CronjobRequest request = new CronjobRequest();
        request.setName("Daily");
        request.setCronValue("0 */5 * * * *");
        return request;
    }

    private CronjobResponse response() {
        return new CronjobResponse(9L, "Daily", "0 */5 * * * *");
    }
}
