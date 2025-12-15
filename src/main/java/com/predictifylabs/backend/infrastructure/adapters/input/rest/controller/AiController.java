package com.predictifylabs.backend.infrastructure.adapters.input.rest.controller;

import com.predictifylabs.backend.application.ports.input.AiServiceUseCase;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.ai.GenerateEventDescriptionRequest;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.ai.GenerateTextRequest;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.ai.GenerateTextResponse;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * REST Controller for AI-powered text generation
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "AI-powered text generation endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AiServiceUseCase aiService;

    @Value("${application.ai.gemini.model:gemini-1.5-flash}")
    private String model;

    @PostMapping("/generate")
    @Operation(summary = "Generate text", description = "Generates text based on a custom prompt using AI")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Text generated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed - prompt is required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "AI service unavailable", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<GenerateTextResponse> generateText(
            @Valid @RequestBody GenerateTextRequest request) {
        String generatedText = aiService.generateText(request.getPrompt());

        return ResponseEntity.ok(GenerateTextResponse.builder()
                .generatedText(generatedText)
                .model(model)
                .generatedAt(LocalDateTime.now())
                .build());
    }

    @PostMapping("/generate/event-description")
    @Operation(summary = "Generate event description", description = "Generates an optimized description for an event using AI")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Description generated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed - event title is required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "AI service unavailable", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<GenerateTextResponse> generateEventDescription(
            @Valid @RequestBody GenerateEventDescriptionRequest request) {
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
        context.append("Title: ").append(request.getEventTitle());

        if (request.getEventType() != null && !request.getEventType().isBlank()) {
            context.append("\nEvent type: ").append(request.getEventType());
        }
        if (request.getTechnologies() != null && !request.getTechnologies().isBlank()) {
            context.append("\nTechnologies: ").append(request.getTechnologies());
        }
        if (request.getAdditionalContext() != null && !request.getAdditionalContext().isBlank()) {
            context.append("\nAdditional context: ").append(request.getAdditionalContext());
        }

        return context.toString();
    }
}
