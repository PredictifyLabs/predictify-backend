package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.user;

import com.predictifylabs.backend.domain.model.Role;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for user profile information
 */
@Builder
public record UserDTO(
    UUID id,
    String name,
    String email,
    String avatar,
    String bio,
    String location,
    Role role,
    Boolean isVerified,
    OffsetDateTime emailVerifiedAt,
    OffsetDateTime lastLoginAt,
    OffsetDateTime createdAt
) {}
