package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.*;
import org.example.service.CronjobExecutionService;
import org.example.service.CronjobService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CronjobControllerTest {
    @Mock
    private CronjobService cronjobService;
    @Mock
    private CronjobExecutionService executionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void updateShouldReadCronjobIdFromBody() throws Exception {
        CronjobUpdateRequest request = new CronjobUpdateRequest();
        request.setId(9L);
        request.setName("Daily");
        request.setCronValue("0 */5 * * * *");
        when(cronjobService.update(eq(9L), any()))
                .thenReturn(new CronjobResponse(
                        9L, "Daily", "0 */5 * * * *"));

        mockMvc().perform(put("/api/cronjobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(9));

        verify(cronjobService).update(eq(9L), any());
    }

    @Test
    void batchStatusShouldReadCronjobIdFromBody() throws Exception {
        BatchStatusItem item = new BatchStatusItem();
        item.setId(11L);
        item.setExpectedStatus(true);
        BatchChangeStatusRequest request = new BatchChangeStatusRequest();
        request.setCronjobId(7L);
        request.setItems(Collections.singletonList(item));
        request.setStatus(false);
        when(executionService.changeAllStatuses(eq(7L), any()))
                .thenReturn(Collections.emptyList());

        mockMvc().perform(patch("/api/cronjobs/executions/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(executionService).changeAllStatuses(eq(7L), any());
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(
                new CronjobController(cronjobService, executionService)).build();
    }
}
