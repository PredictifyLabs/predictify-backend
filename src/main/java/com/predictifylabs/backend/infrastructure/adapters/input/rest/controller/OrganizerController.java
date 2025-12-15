package com.predictifylabs.backend.infrastructure.adapters.input.rest.controller;

import com.predictifylabs.backend.application.service.EventService;
import com.predictifylabs.backend.application.service.OrganizerService;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.event.EventDTO;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.organizer.CreateOrganizerDTO;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.organizer.OrganizerProfileDTO;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.exception.ErrorResponse;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for organizer management
 */
@RestController
@RequestMapping("/api/v1/organizers")
@RequiredArgsConstructor
@Tag(name = "Organizers", description = "Organizer profile management endpoints")
public class OrganizerController {

    private final OrganizerService organizerService;
    private final EventService eventService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get all organizers", description = "Returns all registered organizers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Organizers retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<OrganizerProfileDTO>> getAllOrganizers() {
        return ResponseEntity.ok(organizerService.getAllOrganizers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organizer by ID", description = "Returns an organizer by their UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Organizer retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid UUID format", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Organizer not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrganizerProfileDTO> getOrganizerById(@PathVariable UUID id) {
        return ResponseEntity.ok(organizerService.getOrganizerById(id));
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "Get events by organizer", description = "Returns all events created by the specified organizer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Events retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Organizer not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<EventDTO>> getOrganizerEvents(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventsByOrganizer(id));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user's organizer profile", description = "Returns the organizer profile of the authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Organizer profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User is not an organizer", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrganizerProfileDTO> getMyOrganizerProfile(Authentication auth) {
        UUID userId = extractUserId(auth);
        return ResponseEntity.ok(organizerService.getOrganizerByUserId(userId));
    }

    @GetMapping("/me/check")
    @Operation(summary = "Check if current user is an organizer", description = "Returns true if the authenticated user has an organizer profile")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Check completed successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Boolean> isCurrentUserOrganizer(Authentication auth) {
        UUID userId = extractUserId(auth);
        return ResponseEntity.ok(organizerService.isUserOrganizer(userId));
    }

    @PostMapping
    @Operation(summary = "Create organizer profile", description = "Creates an organizer profile for the authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Organizer profile created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "User already has an organizer profile", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrganizerProfileDTO> createOrganizerProfile(
            @RequestBody @Valid CreateOrganizerDTO dto,
            Authentication auth) {
        UUID userId = extractUserId(auth);
        var created = organizerService.createOrganizer(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/me")
    @Operation(summary = "Update organizer profile", description = "Updates the organizer profile of the authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Organizer profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User is not an organizer", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrganizerProfileDTO> updateOrganizerProfile(
            @RequestBody @Valid CreateOrganizerDTO dto,
            Authentication auth) {
        UUID userId = extractUserId(auth);
        var updated = organizerService.updateOrganizer(userId, dto);
        return ResponseEntity.ok(updated);
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
