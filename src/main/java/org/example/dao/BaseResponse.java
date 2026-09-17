package org.example.dao;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BaseResponse {
    private Integer status;
    private String message;
    private Object data;
}
