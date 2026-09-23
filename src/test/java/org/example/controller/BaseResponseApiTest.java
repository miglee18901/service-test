package org.example.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BaseResponseApiTest {
    @Test
    void statusResponse_knownStatus_returnsMatchingHttpStatus() {
        BaseResponseApi api = new BaseResponseApi();
        int[] statuses = {404, 500, 409, 204, 400, 403};
        HttpStatus[] expected = {
                HttpStatus.NOT_FOUND,
                HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.CONFLICT,
                HttpStatus.NO_CONTENT,
                HttpStatus.BAD_REQUEST,
                HttpStatus.FORBIDDEN
        };

        for (int i = 0; i < statuses.length; i++) {
            BaseResponse body = new BaseResponse(statuses[i], "message", null);

            ResponseEntity<?> response = api.statusResponse(body);

            assertEquals(expected[i], response.getStatusCode());
            assertSame(body, response.getBody());
        }
    }

    @Test
    void statusResponse_unmappedStatus_returnsOk() {
        BaseResponseApi api = new BaseResponseApi();
        BaseResponse body = new BaseResponse(401, "message", null);

        ResponseEntity<?> response = api.statusResponse(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(body, response.getBody());
    }
}