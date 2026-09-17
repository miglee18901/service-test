package org.example.dao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.model.Cronjob;

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
