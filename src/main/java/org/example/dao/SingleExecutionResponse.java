package org.example.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SingleExecutionResponse {
    @JsonIgnore
    private int status = 200;
    private String retn;
    private String description;
    private String responseData;
    private String serverAddress;
    private long requestTime;
    private long responseTime;
    private long processTime;
}
