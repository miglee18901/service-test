package org.example.controller;

import static org.mockito.Mockito.when;

import org.example.repository.ExecutionInfoRepository;
import org.example.service.ExecutionInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;

class ExecutionInfoControllerTest {
    @Test
    void listExecuteVim_configuredOptions_returnsOptions() throws Exception {
        ExecutionInfoService service = mock(ExecutionInfoService.class);
        when(service.listExecutionVim()).thenReturn(new BaseResponse(200, "Get list execute vim success", Arrays.asList("vim-a", "vim-b")));
        ExecutionInfoController controller = new ExecutionInfoController(
                mock(ExecutionInfoRepository.class), service);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/executionInfo/execute-vim")
                        .contentType("application/json;charset=utf-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Get list execute vim success"))
                .andExpect(jsonPath("$.data[0]").value("vim-a"))
                .andExpect(jsonPath("$.data[1]").value("vim-b"));
    }
}
