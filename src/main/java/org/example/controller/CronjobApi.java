package org.example.controller;

import io.swagger.annotations.ApiOperation;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.example.dao.BatchChangeStatusRequest;
import org.example.dao.CronjobExecutionResponse;
import org.example.dao.CronjobRequest;
import org.example.dao.CronjobResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;

@RequestMapping(value = "/cronjobs", produces = {"application/json;charset=utf-8"})
public interface CronjobApi {
    @ApiOperation(value = "Create a cronjob", notes = "Creates a cronjob with a valid six-field cron expression.", response = CronjobResponse.class, tags = {"Cronjob"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "Created successfully", response = CronjobResponse.class),
            @ApiResponse(code = 400, message = "Invalid request", response = BaseResponse.class),
            @ApiResponse(code = 409, message = "Cronjob name already exists", response = BaseResponse.class)
    })
    @RequestMapping(
            method = RequestMethod.POST,
            produces = {"application/json;charset=utf-8"},
            consumes = {"application/json;charset=utf-8"})
    BaseResponse create(@Valid @RequestBody CronjobRequest request);

    @ApiOperation(value = "Get a cronjob by ID", notes = "Returns the cronjob identified by the supplied ID.", response = CronjobResponse.class, tags = {"Cronjob"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = CronjobResponse.class),
            @ApiResponse(code = 404, message = "Cronjob not found", response = BaseResponse.class)
    })
    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.GET,
            produces = {"application/json;charset=utf-8"})
    BaseResponse findById(@PathVariable Long id);

    @ApiOperation(value = "Search cronjobs", notes = "Returns a pageable list of cronjobs filtered by keyword.", response = CronjobResponse.class, responseContainer = "List", tags = {"Cronjob"})
    @ApiResponse(code = 200, message = "Success", response = CronjobResponse.class)
    @RequestMapping(
            method = RequestMethod.GET,
            produces = {"application/json;charset=utf-8"})
    BaseResponse search(
            @RequestParam(defaultValue = "") String keyword,
            Pageable pageable);

    @ApiOperation(value = "Update a cronjob", notes = "Replaces the cronjob data and reschedules it when the cron expression changes.", response = CronjobResponse.class, tags = {"Cronjob"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "Updated successfully", response = CronjobResponse.class),
            @ApiResponse(code = 400, message = "Invalid request", response = BaseResponse.class),
            @ApiResponse(code = 404, message = "Cronjob not found", response = BaseResponse.class),
            @ApiResponse(code = 409, message = "Cronjob name already exists", response = BaseResponse.class)
    })
    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.PUT,
            produces = {"application/json;charset=utf-8"},
            consumes = {"application/json;charset=utf-8"})
    BaseResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CronjobRequest request);

    @ApiOperation(value = "Delete a cronjob", notes = "Deletes a cronjob that has no execution mappings.", response = Void.class, tags = {"Cronjob"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "Deleted successfully"),
            @ApiResponse(code = 404, message = "Cronjob not found", response = BaseResponse.class),
            @ApiResponse(code = 409, message = "Cronjob still has executions", response = BaseResponse.class)
    })
    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.DELETE,
            produces = {"application/json;charset=utf-8"})
    BaseResponse delete(@PathVariable Long id);

    @ApiOperation(value = "Change statuses of cronjob executions", notes = "Updates statuses for multiple execution mappings belonging to a cronjob.", response = CronjobExecutionResponse.class, responseContainer = "List", tags = {"Cronjob"})
    @ApiResponses({
            @ApiResponse(code = 200, message = "Statuses updated successfully", response = CronjobExecutionResponse.class),
            @ApiResponse(code = 400, message = "Invalid request", response = BaseResponse.class),
            @ApiResponse(code = 404, message = "Cronjob or execution not found", response = BaseResponse.class)
    })
    @RequestMapping(
            value = "/{cronjobId}/executions/status",
            method = RequestMethod.PATCH,
            produces = {"application/json;charset=utf-8"},
            consumes = {"application/json;charset=utf-8"})
    BaseResponse changeAllStatuses(
            @PathVariable Long cronjobId,
            @Valid @RequestBody BatchChangeStatusRequest request);
}