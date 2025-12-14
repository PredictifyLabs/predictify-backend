package com.predictifylabs.backend.application.service;

import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.auth.AuthenticationRequest;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.auth.AuthenticationResponse;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.auth.RegisterRequest;
import com.predictifylabs.backend.domain.model.Role;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.RefreshTokenEntity;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.UserEntity;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.RefreshTokenRepository;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.UserRepository;
import com.predictifylabs.backend.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        var user = UserEntity.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.ATTENDEE)
                .build();

        var savedUser = userRepository.save(user);

        var userDetails = new org.springframework.security.core.userdetails.User(
                savedUser.getEmail(),
                savedUser.getPassword(),
                java.util.Collections.emptyList());

        var jwtToken = jwtService.generateToken(userDetails);
        var refreshToken = jwtService.generateRefreshToken(userDetails);

        saveUserRefreshToken(savedUser, refreshToken);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update last login
        user.setLastLoginAt(OffsetDateTime.now());
        user.setFailedLoginAttempts((short) 0);
        userRepository.save(user);

        var userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                java.util.Collections.emptyList());

        var jwtToken = jwtService.generateToken(userDetails);
        var refreshToken = jwtService.generateRefreshToken(userDetails);

        revokeAllUserTokens(user.getId());
        saveUserRefreshToken(user, refreshToken);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    private void saveUserRefreshToken(UserEntity user, String token) {
        var refreshToken = RefreshTokenEntity.builder()
                .user(user)
                .tokenHash(String.valueOf(token.hashCode())) // Simplified - in production use proper hashing
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    private void revokeAllUserTokens(UUID userId) {
        var validTokens = refreshTokenRepository.findAllValidTokenByUser(userId, OffsetDateTime.now());
        validTokens.forEach(token -> token.setRevokedAt(OffsetDateTime.now()));
        refreshTokenRepository.saveAll(validTokens);
    }
}
