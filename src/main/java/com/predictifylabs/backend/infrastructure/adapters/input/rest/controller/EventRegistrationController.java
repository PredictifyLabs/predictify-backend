package com.predictifylabs.backend.infrastructure.adapters.input.rest.controller;

import com.predictifylabs.backend.application.service.EventRegistrationService;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.registration.EventRegistrationDTO;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.exception.ErrorResponse;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for event registration management
 */
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Event Registrations", description = "Event registration management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class EventRegistrationController {

    private final EventRegistrationService registrationService;
    private final UserRepository userRepository;

    @PostMapping("/{eventId}/register")
    @Operation(summary = "Register to an event", description = "Registers the authenticated user to an event")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registration successful"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Already registered or event is full", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EventRegistrationDTO> registerToEvent(
            @PathVariable UUID eventId,
            Authentication auth) {
        UUID userId = extractUserId(auth);
        var registration = registrationService.registerToEvent(eventId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(registration);
    }

    @DeleteMapping("/{eventId}/register")
    @Operation(summary = "Cancel registration", description = "Cancels the authenticated user's registration from an event")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registration cancelled successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Registration not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> cancelRegistration(
            @PathVariable UUID eventId,
            Authentication auth) {
        UUID userId = extractUserId(auth);
        registrationService.cancelRegistration(eventId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/registration")
    @Operation(summary = "Get registration status", description = "Returns the registration status of the authenticated user for an event")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not registered for this event", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EventRegistrationDTO> getRegistrationStatus(
            @PathVariable UUID eventId,
            Authentication auth) {
        UUID userId = extractUserId(auth);
        var registration = registrationService.getRegistration(eventId, userId);
        if (registration == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(registration);
    }

    @GetMapping("/{eventId}/registered")
    @Operation(summary = "Check if registered", description = "Returns true if the authenticated user is registered for the event")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Check completed successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Boolean> isUserRegistered(
            @PathVariable UUID eventId,
            Authentication auth) {
        UUID userId = extractUserId(auth);
        return ResponseEntity.ok(registrationService.isUserRegistered(eventId, userId));
    }

    @GetMapping("/{eventId}/registrations")
    @Operation(summary = "Get all registrations", description = "Returns all registrations for an event (organizer only)")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registrations retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not the event organizer", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<EventRegistrationDTO>> getEventRegistrations(
            @PathVariable UUID eventId) {
        return ResponseEntity.ok(registrationService.getEventRegistrations(eventId));
    }

    @PostMapping("/{eventId}/registrations/{userId}/attendance")
    @Operation(summary = "Mark attendance", description = "Marks a user as having attended the event (organizer only)")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attendance marked successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not the event organizer", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Registration not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EventRegistrationDTO> markAttendance(
            @PathVariable UUID eventId,
            @PathVariable UUID userId) {
        var registration = registrationService.markAttendance(eventId, userId);
        return ResponseEntity.ok(registration);
    }

    /**
     * Extract user ID from authentication
     */
    private UUID extractUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}
