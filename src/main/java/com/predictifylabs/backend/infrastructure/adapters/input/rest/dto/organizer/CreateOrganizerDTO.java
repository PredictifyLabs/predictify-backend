package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.organizer;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating an organizer profile
 */
public record CreateOrganizerDTO(
    @NotBlank(message = "Display name is required")
    String displayName,
    
    String avatar,
    String bio,
    String email,
    String website
) {}
