package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.BaseResponse;
import org.example.dto.BatchChangeStatusRequest;
import org.example.dto.CronjobExecutionResponse;
import org.example.dto.CronjobRequest;
import org.example.dto.CronjobResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "Cronjob", description = "Cronjob management API")
@RequestMapping(value = "/cronjobs", produces = {"application/json;charset=utf-8"})
public interface CronjobApi {
    @Operation(summary = "Create a cronjob")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Cronjob name already exists")
    })
    @RequestMapping(
            method = RequestMethod.POST,
            produces = {"application/json;charset=utf-8"},
            consumes = {"application/json;charset=utf-8"})
    BaseResponse<CronjobResponse> create(@Valid @RequestBody CronjobRequest request);

    @Operation(summary = "Get a cronjob by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Cronjob not found")
    })
    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.GET,
            produces = {"application/json;charset=utf-8"})
    BaseResponse<CronjobResponse> findById(@PathVariable Long id);

    @Operation(summary = "Search cronjobs")
    @ApiResponse(responseCode = "200", description = "Success")
    @RequestMapping(
            method = RequestMethod.GET,
            produces = {"application/json;charset=utf-8"})
    BaseResponse<Page<CronjobResponse>> search(
            @RequestParam(defaultValue = "") String keyword,
            Pageable pageable);

    @Operation(summary = "Update a cronjob")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Cronjob not found"),
            @ApiResponse(responseCode = "409", description = "Cronjob name already exists")
    })
    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.PUT,
            produces = {"application/json;charset=utf-8"},
            consumes = {"application/json;charset=utf-8"})
    BaseResponse<CronjobResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CronjobRequest request);

    @Operation(summary = "Delete a cronjob")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Cronjob not found"),
            @ApiResponse(responseCode = "409", description = "Cronjob still has executions")
    })
    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.DELETE,
            produces = {"application/json;charset=utf-8"})
    BaseResponse<Void> delete(@PathVariable Long id);

    @Operation(summary = "Change statuses of cronjob executions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statuses updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Cronjob or execution not found")
    })
    @RequestMapping(
            value = "/{cronjobId}/executions/status",
            method = RequestMethod.PATCH,
            produces = {"application/json;charset=utf-8"},
            consumes = {"application/json;charset=utf-8"})
    BaseResponse<List<CronjobExecutionResponse>> changeAllStatuses(
            @PathVariable Long cronjobId,
            @Valid @RequestBody BatchChangeStatusRequest request);
}