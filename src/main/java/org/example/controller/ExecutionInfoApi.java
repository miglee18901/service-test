package org.example.controller;

import io.swagger.annotations.ApiOperation;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.BaseResponse;
import org.example.entity.ExecutionInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@Tag(name = "Execution Info", description = "Execution information API")
@RequestMapping(value = "/execution-info", produces = {"application/json;charset=utf-8"})
public interface ExecutionInfoApi {
    @ApiOperation(value = "Get execution information", notes = "Returns a pageable list of execution information.", response = ExecutionInfo.class, responseContainer = "List", tags = {"Execution Info"})
    @ApiResponse(code = 200, message = "Success", response = ExecutionInfo.class)
    @RequestMapping(
            method = RequestMethod.GET,
            produces = {"application/json;charset=utf-8"})
    Page<ExecutionInfo> findAll(Pageable pageable);

    @ApiOperation(value = "Start an execution", notes = "Starts the selected execution for the user supplied in the X-User header.", response = BaseResponse.class, tags = {"Execution Info"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "Start request processed", response = BaseResponse.class),
            @ApiResponse(code = 409, message = "Execution cannot be started", response = BaseResponse.class)
    })
    @RequestMapping(
            value = "/{id}/start",
            method = RequestMethod.POST,
            produces = {"application/json;charset=utf-8"})
    BaseResponse<Map<String, String>> start(
            @PathVariable Long id,
            @RequestHeader(value = "X-User", defaultValue = "anonymous") String username);
}