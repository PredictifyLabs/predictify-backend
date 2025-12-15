package com.predictifylabs.backend.infrastructure.adapters.input.rest.controller;

import com.predictifylabs.backend.application.service.PredictionService;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.prediction.PredictionDTO;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for event prediction management
 */
@RestController
@RequestMapping("/api/v1/predictions")
@RequiredArgsConstructor
@Tag(name = "Predictions", description = "Event attendance prediction endpoints")
public class PredictionController {

    private final PredictionService predictionService;

    @GetMapping("/events/{eventId}")
    @Operation(summary = "Get prediction for an event", description = "Returns the attendance prediction for a specific event")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prediction retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid UUID format", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Prediction not found for this event", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PredictionDTO> getEventPrediction(@PathVariable UUID eventId) {
        var prediction = predictionService.getEventPrediction(eventId);
        if (prediction == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(prediction);
    }

    @PostMapping("/events/{eventId}/generate")
    @Operation(summary = "Generate new prediction", description = "Generates a new AI-powered attendance prediction for an event")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prediction generated successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "AI service unavailable", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PredictionDTO> generatePrediction(@PathVariable UUID eventId) {
        var prediction = predictionService.generatePrediction(eventId);
        return ResponseEntity.ok(prediction);
    }

    @GetMapping("/events/{eventId}/insight")
    @Operation(summary = "Get AI insight", description = "Returns an AI-generated textual insight about the event prediction")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Insight generated successfully"),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "AI service unavailable", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<String> getPredictionInsight(@PathVariable UUID eventId) {
        var insight = predictionService.generatePredictionInsight(eventId);
        return ResponseEntity.ok(insight);
    }
}
