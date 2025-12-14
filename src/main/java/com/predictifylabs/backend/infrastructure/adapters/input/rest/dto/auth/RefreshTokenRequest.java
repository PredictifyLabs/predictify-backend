package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for refresh token request
 */
public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {}
