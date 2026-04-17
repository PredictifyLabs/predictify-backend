package com.predictifylabs.backend.infrastructure.adapters.input.rest.exception;

public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
