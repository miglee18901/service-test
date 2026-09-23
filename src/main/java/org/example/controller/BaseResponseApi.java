package org.example.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.validation.constraints.NotNull;

public class BaseResponseApi {
    protected ResponseEntity<?> statusResponse(@NotNull BaseResponse baseResponse) {
        ResponseEntity<?> responseEntity;
        switch (baseResponse.getStatus()) {
            case 404:
                responseEntity = ResponseEntity.status(HttpStatus.NOT_FOUND).body(baseResponse);
                break;
            case 500:
                responseEntity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(baseResponse);
                break;
            case 409:
                responseEntity = ResponseEntity.status(HttpStatus.CONFLICT).body(baseResponse);
                break;
            case 204:
                responseEntity = ResponseEntity.status(HttpStatus.NO_CONTENT).body(baseResponse);
                break;
            case 400:
                responseEntity = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(baseResponse);
                break;
            case 403:
                responseEntity = ResponseEntity.status(HttpStatus.FORBIDDEN).body(baseResponse);
                break;
            default:
                responseEntity = ResponseEntity.status(HttpStatus.OK).body(baseResponse);
                break;
        }
        return responseEntity;
    }
}