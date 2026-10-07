package com.kku.queuenotify.exception;

import org.springframework.http.HttpStatus;

public class PushDemoException extends RuntimeException {
    private final HttpStatus status;

    public PushDemoException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
