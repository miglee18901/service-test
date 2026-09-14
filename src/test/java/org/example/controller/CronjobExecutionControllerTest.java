package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.*;
import org.example.service.CronjobExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CronjobExecutionControllerTest {
    @Mock
    private CronjobExecutionService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void updateShouldReadMappingIdFromPath() throws Exception {
        CronjobExecutionRequest request = new CronjobExecutionRequest();
        request.setCronjobId(1L);
        request.setExecutionInfoId(10L);
        request.setStatus(true);
        when(service.update(eq(15L), any())).thenReturn(response(true));

        mockMvc().perform(put("/api/cronjob-executions/{id}", 15L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(15));

        verify(service).update(eq(15L), any());
    }

    @Test
    void changeStatusShouldReadMappingIdFromPath() throws Exception {
        ChangeStatusRequest request = new ChangeStatusRequest();
        request.setExpectedStatus(true);
        request.setStatus(false);
        when(service.changeStatus(eq(15L), any())).thenReturn(response(false));

        mockMvc().perform(patch("/api/cronjob-executions/{id}/status", 15L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(false));

        verify(service).changeStatus(eq(15L), any());
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(
                new CronjobExecutionController(service)).build();
    }

    private CronjobExecutionResponse response(boolean status) {
        return new CronjobExecutionResponse(
                15L, 1L, "Daily", 10L, "Execution", status);
    }
}
