package org.example.controller;

import org.example.dao.BaseResponse;
import org.example.model.Category;
import org.example.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryApiControllerTest {
    @Mock
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void clone_validRequest_passesParametersToService() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CategoryApiController(categoryService)).build();
        Category cloned = new Category();
        cloned.setCategoryId(42L);
        cloned.setCategoryName("Copied");
        when(categoryService.clone(7L, 2, "Copied", "Details"))
                .thenReturn(new BaseResponse(200, "Clone successfully", cloned));

        mockMvc.perform(post("/category/{id}/clone", 7L).contentType("application/json;charset=utf-8")
                        .param("categoryType", "2")
                        .param("name", "Copied")
                        .param("description", "Details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.categoryId").value(42))
                .andExpect(jsonPath("$.data.categoryName").value("Copied"));

        verify(categoryService).clone(7L, 2, "Copied", "Details");
    }

    @Test
    void clone_missingDescription_usesEmptyString() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CategoryApiController(categoryService)).build();
        when(categoryService.clone(7L, 2, "Copied", ""))
                .thenReturn(new BaseResponse(200, "Clone successfully", null));

        mockMvc.perform(post("/category/{id}/clone", 7L).contentType("application/json;charset=utf-8")
                        .param("categoryType", "2")
                        .param("name", "Copied"))
                .andExpect(status().isOk());

        verify(categoryService).clone(7L, 2, "Copied", "");
    }

    @Test
    void clone_serviceFailure_returnsErrorResponse() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CategoryApiController(categoryService)).build();
        when(categoryService.clone(7L, 2, "Copied", ""))
                .thenReturn(new BaseResponse(500, "Clone fail! database error", null));

        mockMvc.perform(post("/category/{id}/clone", 7L).contentType("application/json;charset=utf-8")
                        .param("categoryType", "2")
                        .param("name", "Copied"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Clone fail! database error"));
    }

    @Test
    void clone_serviceException_returnsErrorResponse() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CategoryApiController(categoryService)).build();
        when(categoryService.clone(7L, 2, "Copied", ""))
                .thenThrow(new IllegalStateException("unexpected failure"));

        mockMvc.perform(post("/category/{id}/clone", 7L).contentType("application/json;charset=utf-8")
                        .param("categoryType", "2")
                        .param("name", "Copied"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("unexpected failure"));
    }
}