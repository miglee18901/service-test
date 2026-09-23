package org.example.controller;

import org.example.service.TestCaseService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TestCaseApiControllerTest {
    @Test
    void getBlockedValueCharacteristics_validConfiguration_returnsMap() throws Exception {
        TestCaseService service = mock(TestCaseService.class);
        Map<String, List<String>> blockedValues = new LinkedHashMap<>();
        blockedValues.put("600100Msisdn", Arrays.asList("3000", "3001"));
        blockedValues.put("600000BalType", Arrays.asList("3000", "3001"));
        when(service.getBlockedValueCharacteristics())
                .thenReturn(new BaseResponse(200, "Successfully", blockedValues));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestCaseApiController(service)).build();

        mockMvc.perform(get("/test-case/characteristics/blocked-value")
                        .contentType("application/json;charset=utf-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.600100Msisdn[0]").value("3000"))
                .andExpect(jsonPath("$.data.600000BalType[1]").value("3001"));
    }

    @Test
    void getBlockedValueCharacteristics_serviceThrows_returnsServerError() throws Exception {
        TestCaseService service = mock(TestCaseService.class);
        when(service.getBlockedValueCharacteristics()).thenThrow(new IllegalStateException("failure"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestCaseApiController(service)).build();

        mockMvc.perform(get("/test-case/characteristics/blocked-value")
                        .contentType("application/json;charset=utf-8"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("failure"));
    }
}
