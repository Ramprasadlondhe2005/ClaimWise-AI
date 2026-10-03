package com.claimwise.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ApiException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
