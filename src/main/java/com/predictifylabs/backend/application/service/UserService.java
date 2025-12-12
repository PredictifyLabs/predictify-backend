package com.predictifylabs.backend.application.service;

import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.user.UpdateUserDTO;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.user.UserDTO;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.UserEntity;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for user management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get user by ID
     */
    public UserDTO getUserById(UUID userId) {
        return userRepository.findById(userId)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    /**
     * Get user by email
     */
    public UserDTO getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    /**
     * Get current user profile
     */
    public UserDTO getCurrentUser(String email) {
        return getUserByEmail(email);
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserDTO updateUser(UUID userId, UpdateUserDTO dto) {
        log.info("Updating user profile for user {}", userId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (dto.name() != null) user.setName(dto.name());
        if (dto.avatar() != null) user.setAvatar(dto.avatar());
        if (dto.bio() != null) user.setBio(dto.bio());
        if (dto.location() != null) user.setLocation(dto.location());

        var saved = userRepository.save(user);
        log.info("User profile updated for user {}", userId);
        return toDTO(saved);
    }

    /**
     * Get all users (admin only)
     */
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Deactivate user account
     */
    @Transactional
    public void deactivateUser(UUID userId) {
        log.info("Deactivating user {}", userId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setIsActive(false);
        userRepository.save(user);
        log.info("User {} deactivated", userId);
    }

    /**
     * Reactivate user account
     */
    @Transactional
    public void reactivateUser(UUID userId) {
        log.info("Reactivating user {}", userId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setIsActive(true);
        userRepository.save(user);
        log.info("User {} reactivated", userId);
    }

    // Helper methods
    private UserDTO toDTO(UserEntity user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .location(user.getLocation())
                .role(user.getRole())
                .isVerified(user.getIsVerified())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
