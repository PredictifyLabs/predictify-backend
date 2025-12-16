package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Authentication response containing JWT tokens")
public class AuthenticationResponse {

    @JsonProperty("access_token")
    @Schema(description = "JWT access token for API authentication (expires in 24 hours)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNjE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
    private String accessToken;

    @JsonProperty("refresh_token")
    @Schema(description = "JWT refresh token to obtain new access tokens (expires in 7 days)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwidHlwZSI6InJlZnJlc2gifQ.drt_po6bHhDvF3yKYNQ8RRv9MuKfHvcGN6r7SfY7dMs")
    private String refreshToken;
}
