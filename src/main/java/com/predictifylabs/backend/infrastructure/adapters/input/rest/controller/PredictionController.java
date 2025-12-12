package com.predictifylabs.backend.infrastructure.adapters.input.rest.controller;

import com.predictifylabs.backend.application.service.PredictionService;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.prediction.PredictionDTO;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Get prediction for an event")
    public ResponseEntity<PredictionDTO> getEventPrediction(@PathVariable UUID eventId) {
        var prediction = predictionService.getEventPrediction(eventId);
        if (prediction == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(prediction);
    }

    @PostMapping("/events/{eventId}/generate")
    @Operation(summary = "Generate new prediction for an event")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PredictionDTO> generatePrediction(@PathVariable UUID eventId) {
        var prediction = predictionService.generatePrediction(eventId);
        return ResponseEntity.ok(prediction);
    }

    @GetMapping("/events/{eventId}/insight")
    @Operation(summary = "Get AI-generated insight for an event prediction")
    public ResponseEntity<String> getPredictionInsight(@PathVariable UUID eventId) {
        var insight = predictionService.generatePredictionInsight(eventId);
        return ResponseEntity.ok(insight);
    }
}
