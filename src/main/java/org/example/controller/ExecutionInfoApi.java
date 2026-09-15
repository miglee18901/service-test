package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "Get execution information")
    @ApiResponse(responseCode = "200", description = "Success")
    @RequestMapping(
            method = RequestMethod.GET,
            produces = {"application/json;charset=utf-8"})
    Page<ExecutionInfo> findAll(Pageable pageable);

    @Operation(summary = "Start an execution")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Start request processed"),
            @ApiResponse(responseCode = "409", description = "Execution cannot be started")
    })
    @RequestMapping(
            value = "/{id}/start",
            method = RequestMethod.POST,
            produces = {"application/json;charset=utf-8"})
    BaseResponse<Map<String, String>> start(
            @PathVariable Long id,
            @RequestHeader(value = "X-User", defaultValue = "anonymous") String username);
}