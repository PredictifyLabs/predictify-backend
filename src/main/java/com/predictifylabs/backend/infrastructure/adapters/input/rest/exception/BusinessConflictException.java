package com.predictifylabs.backend.infrastructure.adapters.input.rest.exception;

public class BusinessConflictException extends RuntimeException {
    public BusinessConflictException(String message) {
        super(message);
    }
}
