package com.predictifylabs.backend.infrastructure.adapters.input.rest.exception;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Reusable API error response annotations for Swagger documentation
 */
public class ApiErrors {

    // ==================== COMMON ERROR RESPONSES ====================

    @Target({ ElementType.METHOD, ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Bad Request - Validation failed or invalid input", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Validation Error", value = """
                    {
                      "timestamp": "2025-12-15T17:30:00",
                      "status": 400,
                      "error": "Bad Request",
                      "message": "Validation failed",
                      "path": "/api/v1/auth/register",
                      "errors": {
                        "email": "Invalid email format",
                        "password": "Password must be between 8 and 100 characters"
                      }
                    }
                    """))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Server Error", value = """
                    {
                      "timestamp": "2025-12-15T17:30:00",
                      "status": 500,
                      "error": "Internal Server Error",
                      "message": "An unexpected error occurred. Please try again later.",
                      "path": "/api/v1/resource"
                    }
                    """)))
    })
    public @interface CommonErrors {
    }

    // ==================== AUTHENTICATION ERRORS ====================

    @Target({ ElementType.METHOD, ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid credentials or missing token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Unauthorized", value = """
                    {
                      "timestamp": "2025-12-15T17:30:00",
                      "status": 401,
                      "error": "Unauthorized",
                      "message": "Invalid email or password",
                      "path": "/api/v1/auth/login"
                    }
                    """))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Access denied", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Forbidden", value = """
                    {
                      "timestamp": "2025-12-15T17:30:00",
                      "status": 403,
                      "error": "Forbidden",
                      "message": "Access denied. You don't have permission to access this resource.",
                      "path": "/api/v1/admin/users"
                    }
                    """)))
    })
    public @interface AuthErrors {
    }

    // ==================== RESOURCE ERRORS ====================

    @Target({ ElementType.METHOD, ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Not Found - Resource does not exist", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Not Found", value = """
                    {
                      "timestamp": "2025-12-15T17:30:00",
                      "status": 404,
                      "error": "Not Found",
                      "message": "User not found with id: 123e4567-e89b-12d3-a456-426614174000",
                      "path": "/api/v1/users/123e4567-e89b-12d3-a456-426614174000"
                    }
                    """))),
            @ApiResponse(responseCode = "409", description = "Conflict - Resource already exists or state conflict", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Conflict", value = """
                    {
                      "timestamp": "2025-12-15T17:30:00",
                      "status": 409,
                      "error": "Conflict",
                      "message": "Email already registered: user@example.com",
                      "path": "/api/v1/auth/register"
                    }
                    """)))
    })
    public @interface ResourceErrors {
    }

    // ==================== COMBINED: ALL AUTH ENDPOINT ERRORS ====================

    @Target({ ElementType.METHOD, ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @CommonErrors
    @AuthErrors
    @ResourceErrors
    public @interface AllAuthErrors {
    }

    // ==================== COMBINED: CRUD ENDPOINT ERRORS ====================

    @Target({ ElementType.METHOD, ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @CommonErrors
    @AuthErrors
    @ResourceErrors
    public @interface CrudErrors {
    }
}
