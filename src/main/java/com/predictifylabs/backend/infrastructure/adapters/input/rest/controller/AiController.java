package com.predictifylabs.backend.infrastructure.adapters.input.rest.controller;

import com.predictifylabs.backend.application.ports.input.AiServiceUseCase;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.ai.GenerateEventDescriptionRequest;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.ai.GenerateTextRequest;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.ai.GenerateTextResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiServiceUseCase aiService;

    @Value("${application.ai.gemini.model:gemini-1.5-flash}")
    private String model;

    /**
     * Genera texto libre basado en un prompt personalizado.
     * POST /api/v1/ai/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<GenerateTextResponse> generateText(
            @Valid @RequestBody GenerateTextRequest request
    ) {
        String generatedText = aiService.generateText(request.getPrompt());

        return ResponseEntity.ok(GenerateTextResponse.builder()
                .generatedText(generatedText)
                .model(model)
                .generatedAt(LocalDateTime.now())
                .build());
    }

    /**
     * Genera una descripción optimizada para un evento.
     * POST /api/v1/ai/generate/event-description
     */
    @PostMapping("/generate/event-description")
    public ResponseEntity<GenerateTextResponse> generateEventDescription(
            @Valid @RequestBody GenerateEventDescriptionRequest request
    ) {
        String context = buildEventContext(request);
        String generatedText = aiService.generateEventDescription(context);

        return ResponseEntity.ok(GenerateTextResponse.builder()
                .generatedText(generatedText)
                .model(model)
                .generatedAt(LocalDateTime.now())
                .build());
    }

    private String buildEventContext(GenerateEventDescriptionRequest request) {
        StringBuilder context = new StringBuilder();
        context.append("Título: ").append(request.getEventTitle());

        if (request.getEventType() != null && !request.getEventType().isBlank()) {
            context.append("\nTipo de evento: ").append(request.getEventType());
        }
        if (request.getTechnologies() != null && !request.getTechnologies().isBlank()) {
            context.append("\nTecnologías: ").append(request.getTechnologies());
        }
        if (request.getAdditionalContext() != null && !request.getAdditionalContext().isBlank()) {
            context.append("\nContexto adicional: ").append(request.getAdditionalContext());
        }

        return context.toString();
    }
}
