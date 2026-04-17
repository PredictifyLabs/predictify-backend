package com.predictifylabs.backend.infrastructure.adapters.input.rest.exception;

public final class ErrorCodes {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String REQUEST_INVALID = "REQUEST_INVALID";

    public static final String AUTH_INVALID_TOKEN = "AUTH_INVALID_TOKEN";
    public static final String AUTH_TOKEN_EXPIRED = "AUTH_TOKEN_EXPIRED";
    public static final String AUTH_FAILED = "AUTH_FAILED";
    public static final String AUTH_FORBIDDEN = "AUTH_FORBIDDEN";

    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String BUSINESS_CONFLICT = "BUSINESS_CONFLICT";

    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private ErrorCodes() {
    }
}
