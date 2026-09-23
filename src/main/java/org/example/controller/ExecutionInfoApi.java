package org.example.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.example.model.ExecutionInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

public interface ExecutionInfoApi {
    @ApiOperation(value = "Get execution information", notes = "Returns a pageable list of execution information.",
            response = ExecutionInfo.class, responseContainer = "List", tags = {"Execution Info"})
    @ApiResponse(code = 200, message = "Success", response = ExecutionInfo.class)
    @RequestMapping(value = "/execution-info",
            method = RequestMethod.GET,
            produces = {"application/json;charset=utf-8"})
    Page<ExecutionInfo> findAll(Pageable pageable);

    @ApiOperation(value = "Start an execution",
            notes = "Starts the selected execution for the user supplied in the X-User header.",
            response = BaseResponse.class, tags = {"Execution Info"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "Start request processed", response = BaseResponse.class),
            @ApiResponse(code = 409, message = "Execution cannot be started", response = BaseResponse.class)
    })
    @RequestMapping(value = "/execution-info/{id}/start",
            method = RequestMethod.POST,
            produces = {"application/json;charset=utf-8"})
    BaseResponse start(
            @PathVariable Long id,
            @RequestHeader(value = "X-User", defaultValue = "anonymous") String username);

    @ApiOperation(value = "List execute vim", notes = "This operation list execute vim",
            response = List.class, responseContainer = "List", tags = {"String"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = List.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Bad Request", response = BaseResponse.class),
            @ApiResponse(code = 401, message = "Unauthorized", response = BaseResponse.class),
            @ApiResponse(code = 403, message = "Forbidden", response = BaseResponse.class),
            @ApiResponse(code = 404, message = "Not Found", response = BaseResponse.class),
            @ApiResponse(code = 405, message = "Method Not allowed", response = BaseResponse.class),
            @ApiResponse(code = 409, message = "Conflict", response = BaseResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = BaseResponse.class)
    })
    @RequestMapping(value = "/executionInfo/execute-vim",
            produces = {"application/json;charset=utf-8"},
            consumes = {"application/json;charset=utf-8"},
            method = RequestMethod.GET)
    ResponseEntity<?> listExecuteVim();
}
