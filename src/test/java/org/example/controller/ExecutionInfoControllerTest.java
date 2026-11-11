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
    void executeBatch_delegatesAndUsesResponseHttpStatus() throws Exception {
        org.example.service.BatchExecutionService batch = mock(org.example.service.BatchExecutionService.class);
        org.example.dao.BatchExecutionResponse response = new org.example.dao.BatchExecutionResponse();
        response.setStatus(401);
        response.setRetn("NOT_AUTHEN");
        when(batch.executeBatch(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("operator")))
                .thenReturn(response);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ExecutionInfoController(
                mock(ExecutionInfoRepository.class), mock(ExecutionInfoService.class), batch)).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/executionInfo/execute-batch")
                        .header("X-User", "operator").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.retn").value("NOT_AUTHEN"));
    }

    @Test
    void listExecuteVim_configuredOptions_returnsOptions() throws Exception {
        ExecutionInfoService service = mock(ExecutionInfoService.class);
        when(service.listExecutionVim()).thenReturn(new BaseResponse(200, "Get list execute vim success", Arrays.asList("vim-a", "vim-b")));
        ExecutionInfoController controller = new ExecutionInfoController(
                mock(ExecutionInfoRepository.class), service, mock(org.example.service.BatchExecutionService.class));
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
