package com.yasirkhan.schedule.exceptions;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends RuntimeException {

    private String message;
    private HttpStatus status;

    public ResourceNotFoundException(String message) {
        super(message);
        this.message = message;
        this.status = HttpStatus.NOT_FOUND;
    }
}
