package com.predictifylabs.backend.infrastructure.adapters.input.rest.controller;

import com.predictifylabs.backend.application.service.EventRegistrationService;
import com.predictifylabs.backend.application.service.UserService;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.registration.EventRegistrationDTO;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.user.UpdateUserDTO;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.user.UserDTO;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for user profile management
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management endpoints")
public class UserController {

    private final UserService userService;
    private final EventRegistrationService registrationService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication auth) {
        return ResponseEntity.ok(userService.getCurrentUser(auth.getName()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> updateCurrentUser(
            @RequestBody @Valid UpdateUserDTO dto,
            Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        var updated = userService.updateUser(userId, dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/me/registrations")
    @Operation(summary = "Get current user's event registrations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EventRegistrationDTO>> getMyRegistrations(Authentication auth) {
        UUID userId = extractUserId(auth);
        return ResponseEntity.ok(registrationService.getUserRegistrations(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    @Operation(summary = "Get all users (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a user account (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate a user account (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reactivateUser(@PathVariable UUID id) {
        userService.reactivateUser(id);
        return ResponseEntity.noContent().build();
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
