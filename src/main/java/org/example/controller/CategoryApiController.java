package org.example.controller;

import org.example.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CategoryApiController extends BaseResponseApi implements CategoryApi {
    private final CategoryService categoryService;

    public CategoryApiController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    public ResponseEntity<?> clone(long id,
                                               Integer categoryType,
                                               String name,
                                               String description,
                                               String accept) {
        try {
            BaseResponse result = categoryService.clone(id, categoryType, name, description);
            return statusResponse(result);
        } catch (Exception e) {
            BaseResponse error = new BaseResponse(500, e.getMessage(), null);
            return statusResponse(error);
        }
    }
}