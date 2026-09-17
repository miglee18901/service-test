package org.example.controller;

import io.swagger.annotations.ApiOperation;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.example.dao.BaseResponse;
import org.example.dao.ChangeStatusRequest;
import org.example.dao.CronjobExecutionRequest;
import org.example.dao.CronjobExecutionResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;

@RequestMapping(value = "/cronjob-executions", produces = {"application/json;charset=utf-8"})
public interface CronjobExecutionApi {
    @ApiOperation(value = "Create a cronjob execution mapping", notes = "Associates an execution with a cronjob.", response = CronjobExecutionResponse.class, tags = {"Cronjob Execution"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "Created successfully", response = CronjobExecutionResponse.class),
            @ApiResponse(code = 400, message = "Invalid request", response = BaseResponse.class),
            @ApiResponse(code = 404, message = "Cronjob or execution not found", response = BaseResponse.class),
            @ApiResponse(code = 409, message = "Mapping already exists", response = BaseResponse.class)
    })
    @RequestMapping(
            method = RequestMethod.POST,
            produces = {"application/json;charset=utf-8"},
            consumes = {"application/json;charset=utf-8"})
    BaseResponse create(@Valid @RequestBody CronjobExecutionRequest request);

    @ApiOperation(value = "Get a cronjob execution mapping by ID", notes = "Returns the execution mapping identified by the supplied ID.", response = CronjobExecutionResponse.class, tags = {"Cronjob Execution"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = CronjobExecutionResponse.class),
            @ApiResponse(code = 404, message = "Mapping not found", response = BaseResponse.class)
    })
    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.GET,
            produces = {"application/json;charset=utf-8"})
    BaseResponse findById(@PathVariable Long id);

    @ApiOperation(value = "Search cronjob execution mappings", notes = "Returns pageable execution mappings filtered by the supplied criteria.", response = CronjobExecutionResponse.class, responseContainer = "List", tags = {"Cronjob Execution"})
    @ApiResponse(code = 200, message = "Success", response = CronjobExecutionResponse.class)
    @RequestMapping(method = RequestMethod.GET, produces = {"application/json;charset=utf-8"})
    BaseResponse search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long cronjobId,
            @RequestParam(required = false) Long executionInfoId,
            @RequestParam(required = false) Boolean status,
            Pageable pageable);

    @ApiOperation(value = "Update a cronjob execution mapping", notes = "Replaces the cronjob and execution association.", response = CronjobExecutionResponse.class, tags = {"Cronjob Execution"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "Updated successfully", response = CronjobExecutionResponse.class),
            @ApiResponse(code = 400, message = "Invalid request", response = BaseResponse.class),
            @ApiResponse(code = 404, message = "Mapping not found", response = BaseResponse.class),
            @ApiResponse(code = 409, message = "Mapping already exists", response = BaseResponse.class)
    })
    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.PUT,
            produces = {"application/json;charset=utf-8"},
            consumes = {"application/json;charset=utf-8"})
    BaseResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CronjobExecutionRequest request);

    @ApiOperation(value = "Delete a cronjob execution mapping", notes = "Deletes the execution mapping identified by the supplied ID.", response = Void.class, tags = {"Cronjob Execution"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "Deleted successfully"),
            @ApiResponse(code = 404, message = "Mapping not found", response = BaseResponse.class)
    })
    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.DELETE,
            produces = {"application/json;charset=utf-8"})
    BaseResponse delete(@PathVariable Long id);

    @ApiOperation(value = "Change a cronjob execution status", notes = "Enables or disables a cronjob execution mapping.", response = CronjobExecutionResponse.class, tags = {"Cronjob Execution"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "Status updated successfully", response = CronjobExecutionResponse.class),
            @ApiResponse(code = 400, message = "Invalid request", response = BaseResponse.class),
            @ApiResponse(code = 404, message = "Mapping not found", response = BaseResponse.class)
    })
    @RequestMapping(
            value = "/{id}/status",
            method = RequestMethod.PATCH,
            produces = {"application/json;charset=utf-8"},
            consumes = {"application/json;charset=utf-8"})
    BaseResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request);
}