package org.example.controller;

import org.example.dao.BaseResponse;
import org.example.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CategoryApiController implements CategoryApi {
    private final CategoryService categoryService;

    public CategoryApiController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    public ResponseEntity<BaseResponse> clone(long id,
                                               Integer categoryType,
                                               String name,
                                               String description,
                                               String accept) {
        try {
            BaseResponse result = categoryService.clone(id, categoryType, name, description);
            return ResponseEntity.status(result.getStatus()).body(result);
        } catch (Exception e) {
            BaseResponse error = new BaseResponse(500, e.getMessage(), null);
            return ResponseEntity.status(500).body(error);
        }
    }
}