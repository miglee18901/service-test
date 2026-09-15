package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.BaseResponse;
import org.example.dto.ChangeStatusRequest;
import org.example.dto.CronjobExecutionRequest;
import org.example.dto.CronjobExecutionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;

@Tag(name = "Cronjob Execution", description = "Cronjob execution mapping API")
@RequestMapping(value = "/cronjob-executions", produces = {"application/json;charset=utf-8"})
public interface CronjobExecutionApi {
    @Operation(summary = "Create a cronjob execution mapping")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Cronjob or execution not found"),
            @ApiResponse(responseCode = "409", description = "Mapping already exists")
    })
    @RequestMapping(
            method = RequestMethod.POST,
            produces = {"application/json;charset=utf-8"},
            consumes = {"application/json;charset=utf-8"})
    BaseResponse<CronjobExecutionResponse> create(@Valid @RequestBody CronjobExecutionRequest request);

    @Operation(summary = "Get a cronjob execution mapping by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Mapping not found")
    })
    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.GET,
            produces = {"application/json;charset=utf-8"})
    BaseResponse<CronjobExecutionResponse> findById(@PathVariable Long id);

    @Operation(summary = "Search cronjob execution mappings")
    @ApiResponse(responseCode = "200", description = "Success")
    @RequestMapping(method = RequestMethod.GET, produces = {"application/json;charset=utf-8"})
    BaseResponse<Page<CronjobExecutionResponse>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long cronjobId,
            @RequestParam(required = false) Long executionInfoId,
            @RequestParam(required = false) Boolean status,
            Pageable pageable);

    @Operation(summary = "Update a cronjob execution mapping")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Mapping not found"),
            @ApiResponse(responseCode = "409", description = "Mapping already exists")
    })
    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.PUT,
            produces = {"application/json;charset=utf-8"},
            consumes = {"application/json;charset=utf-8"})
    BaseResponse<CronjobExecutionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CronjobExecutionRequest request);

    @Operation(summary = "Delete a cronjob execution mapping")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Mapping not found")
    })
    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.DELETE,
            produces = {"application/json;charset=utf-8"})
    BaseResponse<Void> delete(@PathVariable Long id);

    @Operation(summary = "Change a cronjob execution status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Mapping not found")
    })
    @RequestMapping(
            value = "/{id}/status",
            method = RequestMethod.PATCH,
            produces = {"application/json;charset=utf-8"},
            consumes = {"application/json;charset=utf-8"})
    BaseResponse<CronjobExecutionResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request);
}