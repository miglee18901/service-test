package org.example.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.example.model.Category;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

public interface CategoryApi {
    @ApiOperation(value = "Clone category by category type",
            notes = "Clone category by category type",
            response = Category.class,
            tags = {"Category"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = Category.class),
            @ApiResponse(code = 400, message = "Bad Request", response = BaseResponse.class),
            @ApiResponse(code = 401, message = "Unauthorized", response = BaseResponse.class),
            @ApiResponse(code = 403, message = "Forbidden", response = BaseResponse.class),
            @ApiResponse(code = 404, message = "Not Found", response = BaseResponse.class),
            @ApiResponse(code = 405, message = "Method Not allowed", response = BaseResponse.class),
            @ApiResponse(code = 409, message = "Conflict", response = BaseResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = BaseResponse.class)
    })
    @RequestMapping(value = "/category/{id}/clone",
            produces = {"application/json;charset=utf-8"},
            consumes = {"application/json;charset=utf-8"},
            method = RequestMethod.POST)
    ResponseEntity<?> clone(
            @ApiParam(value = "Identifier of the Category", required = true)
            @PathVariable("id") long id,
            @ApiParam(value = "Category Type", required = true)
            @RequestParam("categoryType") Integer categoryType,
            @RequestParam("name") String name,
            @RequestParam(value = "description", defaultValue = "") String description,
            @RequestHeader(value = "Accept", required = false) String accept);
}