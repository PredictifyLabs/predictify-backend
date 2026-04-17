package com.predictifylabs.backend.infrastructure.adapters.input.rest.exception;

public class AuthTokenExpiredException extends RuntimeException {
    public AuthTokenExpiredException(String message) {
        super(message);
    }
}
