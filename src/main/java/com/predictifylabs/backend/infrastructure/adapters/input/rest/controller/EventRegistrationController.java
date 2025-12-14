package com.predictifylabs.backend.infrastructure.adapters.input.rest.controller;

import com.predictifylabs.backend.application.service.EventRegistrationService;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.registration.EventRegistrationDTO;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
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
public class EventRegistrationController {

    private final EventRegistrationService registrationService;
    private final UserRepository userRepository;

    @PostMapping("/{eventId}/register")
    @Operation(summary = "Register to an event")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventRegistrationDTO> registerToEvent(
            @PathVariable UUID eventId,
            Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        var registration = registrationService.registerToEvent(eventId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(registration);
    }

    @DeleteMapping("/{eventId}/register")
    @Operation(summary = "Cancel registration from an event")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelRegistration(
            @PathVariable UUID eventId,
            Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        registrationService.cancelRegistration(eventId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/registration")
    @Operation(summary = "Get registration status for current user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventRegistrationDTO> getRegistrationStatus(
            @PathVariable UUID eventId,
            Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        var registration = registrationService.getRegistration(eventId, userId);
        if (registration == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(registration);
    }

    @GetMapping("/{eventId}/registered")
    @Operation(summary = "Check if current user is registered")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> isUserRegistered(
            @PathVariable UUID eventId,
            Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        return ResponseEntity.ok(registrationService.isUserRegistered(eventId, userId));
    }

    @GetMapping("/{eventId}/registrations")
    @Operation(summary = "Get all registrations for an event (organizer only)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EventRegistrationDTO>> getEventRegistrations(
            @PathVariable UUID eventId
    ) {
        return ResponseEntity.ok(registrationService.getEventRegistrations(eventId));
    }

    @PostMapping("/{eventId}/registrations/{userId}/attendance")
    @Operation(summary = "Mark attendance for a registration (organizer only)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventRegistrationDTO> markAttendance(
            @PathVariable UUID eventId,
            @PathVariable UUID userId
    ) {
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
