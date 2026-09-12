package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.entity.Cronjob;

@Getter
@AllArgsConstructor
public class CronjobResponse {
    private Long id;
    private String name;
    private String cronValue;

    public static CronjobResponse from(Cronjob cronjob) {
        return new CronjobResponse(cronjob.getId(), cronjob.getName(), cronjob.getCronValue());
    }
}
