package org.example.dao;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ExecuteResponse {
    private Integer status;
    private String response;
    private String inputData;
    private long processTime;
}
