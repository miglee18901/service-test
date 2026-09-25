package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dao.*;
import org.example.service.CronjobExecutionService;
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

class CronjobExecutionControllerTest {
    @Mock
    private CronjobExecutionService service;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CronjobExecutionController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        controller = new CronjobExecutionController(service);
    }

    @Test
    void create_validRequest_returnsCreatedResponse() {
        CronjobExecutionRequest request = request();
        CronjobExecutionResponse expected = response(true);
        when(service.create(request)).thenReturn(expected);

        BaseResponse result = controller.create(request);

        assertEquals(HttpStatus.OK.value(), result.getStatus());
        assertEquals("Created successfully", result.getMessage());
        assertEquals(expected, result.getData());
    }

    @Test
    void findById_existingMapping_returnsResponse() {
        CronjobExecutionResponse expected = response(true);
        when(service.findById(15L)).thenReturn(expected);

        BaseResponse result = controller.findById(15L);

        assertEquals("Success", result.getMessage());
        assertEquals(expected, result.getData());
    }

    @Test
    void search_validFiltersAndPageable_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("status"));
        Page<CronjobExecutionResponse> expected =
                new PageImpl<>(Collections.singletonList(response(true)));
        when(service.search("daily", 1L, 10L, true, pageable)).thenReturn(expected);

        BaseResponse result = controller.search("daily", 1L, 10L, true, pageable);

        assertEquals(expected, result.getData());
        verify(service).search("daily", 1L, 10L, true, pageable);
    }

    @Test
    void delete_existingMapping_returnsSuccess() {
        BaseResponse result = controller.delete(15L);

        assertEquals(HttpStatus.OK.value(), result.getStatus());
        assertEquals("Deleted successfully", result.getMessage());
        assertEquals(null, result.getData());
        verify(service).delete(15L);
    }

    @Test
    void update_mappingIdInPath_passesIdToService() throws Exception {
        CronjobExecutionRequest request = new CronjobExecutionRequest();
        request.setCronjobId(1L);
        request.setExecutionInfoId(10L);
        request.setStatus(true);
        when(service.update(eq(15L), any())).thenReturn(response(true));

        mockMvc().perform(put("/cronjob-executions/{id}", 15L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(15));

        verify(service).update(eq(15L), any());
    }

    @Test
    void changeStatus_mappingIdInPath_passesIdToService() throws Exception {
        ChangeStatusRequest request = new ChangeStatusRequest();
        request.setExpectedStatus(true);
        request.setStatus(false);
        when(service.changeStatus(eq(15L), any())).thenReturn(response(false));

        mockMvc().perform(patch("/cronjob-executions/{id}/status", 15L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(false));

        verify(service).changeStatus(eq(15L), any());
    }

    @Test
    void search_negativePage_rejectsRequest() {
        Pageable pageable = mock(Pageable.class);
        when(pageable.getPageNumber()).thenReturn(-1);
        CronjobExecutionController controller = new CronjobExecutionController(service);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.search("", null, null, null, pageable));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Page must be >= 0 and size must be between 1 and 100", exception.getReason());
    }

    @Test
    void search_zeroPageSize_rejectsRequest() {
        Pageable pageable = mock(Pageable.class);
        when(pageable.getPageNumber()).thenReturn(0);
        when(pageable.getPageSize()).thenReturn(0);
        CronjobExecutionController controller = new CronjobExecutionController(service);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.search("", null, null, null, pageable));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Page must be >= 0 and size must be between 1 and 100", exception.getReason());
    }

    @Test
    void search_pageSizeOverMaximum_rejectsRequest() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.search("", null, null, null, PageRequest.of(0, 101)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void search_unsupportedSort_rejectsRequest() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.search("", null, null, null,
                        PageRequest.of(0, 10, Sort.by("unsupported"))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Unsupported sort property: unsupported", exception.getReason());
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(
                controller).build();
    }

    private CronjobExecutionRequest request() {
        CronjobExecutionRequest request = new CronjobExecutionRequest();
        request.setCronjobId(1L);
        request.setExecutionInfoId(10L);
        request.setStatus(true);
        return request;
    }

    private CronjobExecutionResponse response(boolean status) {
        return new CronjobExecutionResponse(
                15L, 1L, "Daily", 10L, "Execution", status);
    }
}
