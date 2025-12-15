package com.predictifylabs.backend.infrastructure.adapters.input.rest.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response for REST API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response")
public class ErrorResponse {

    @Schema(description = "Timestamp of the error", example = "2025-12-15T17:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "HTTP error type", example = "Bad Request")
    private String error;

    @Schema(description = "Error message", example = "Validation failed")
    private String message;

    @Schema(description = "Request path", example = "/api/v1/auth/register")
    private String path;

    @Schema(description = "Field-specific validation errors")
    private Map<String, String> errors;
}
