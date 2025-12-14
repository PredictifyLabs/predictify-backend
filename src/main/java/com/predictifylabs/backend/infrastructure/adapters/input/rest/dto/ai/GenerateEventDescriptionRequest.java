package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenerateEventDescriptionRequest {

    @NotBlank(message = "Event title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String eventTitle;

    @Size(max = 100, message = "Event type cannot exceed 100 characters")
    private String eventType; // Workshop, Meetup, Conference, etc.

    @Size(max = 500, message = "Technologies cannot exceed 500 characters")
    private String technologies; // Java, Spring Boot, etc.

    @Size(max = 1000, message = "Additional context cannot exceed 1000 characters")
    private String additionalContext;
}
