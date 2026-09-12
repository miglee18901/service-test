package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.entity.CronjobExecution;

@Getter
@AllArgsConstructor
public class CronjobExecutionResponse {
    private Long id;
    private Long cronjobId;
    private String cronjobName;
    private Long executionInfoId;
    private String executionInfoName;
    private Boolean status;

    public static CronjobExecutionResponse from(CronjobExecution mapping) {
        return new CronjobExecutionResponse(
                mapping.getId(),
                mapping.getCronjob().getId(),
                mapping.getCronjob().getName(),
                mapping.getExecutionInfo().getId(),
                mapping.getExecutionInfo().getName(),
                mapping.getStatus());
    }
}
