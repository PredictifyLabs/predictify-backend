package com.predictifylabs.backend.infrastructure.adapters.input.rest.exception;

public class AuthTokenInvalidException extends RuntimeException {
    public AuthTokenInvalidException(String message) {
        super(message);
    }
}
