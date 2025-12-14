package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.user;

/**
 * DTO for updating user profile
 */
public record UpdateUserDTO(
    String name,
    String avatar,
    String bio,
    String location
) {}
