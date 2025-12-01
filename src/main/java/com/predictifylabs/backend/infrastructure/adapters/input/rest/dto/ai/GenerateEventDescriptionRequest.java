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

    @NotBlank(message = "El título del evento es requerido")
    @Size(max = 200, message = "El título no puede exceder 200 caracteres")
    private String eventTitle;

    @Size(max = 100, message = "El tipo de evento no puede exceder 100 caracteres")
    private String eventType; // Workshop, Meetup, Conferencia, etc.

    @Size(max = 500, message = "Las tecnologías no pueden exceder 500 caracteres")
    private String technologies; // Java, Spring Boot, etc.

    @Size(max = 1000, message = "El contexto adicional no puede exceder 1000 caracteres")
    private String additionalContext;
}
