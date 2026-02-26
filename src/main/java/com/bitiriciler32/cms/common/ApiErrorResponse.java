package com.bitiriciler32.cms.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

/**
 * Standard API error response returned for all error scenarios.
 */
@Getter
@AllArgsConstructor
public class ApiErrorResponse {
    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    public ApiErrorResponse(int status, String error, String message, String path) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
}
