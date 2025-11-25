package com.predictifylabs.backend.application.service;

import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.auth.AuthenticationRequest;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.auth.AuthenticationResponse;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.auth.RegisterRequest;
import com.predictifylabs.backend.domain.model.Role;
import com.predictifylabs.backend.domain.model.TokenType;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.TokenEntity;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.UserEntity;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.TokenRepository;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.UserRepository;
import com.predictifylabs.backend.infrastructure.security.JwtService;
import com.predictifylabs.backend.infrastructure.config.ApplicationConfig; // Para UserDetails impl
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository repository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        var user = UserEntity.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.ROLE_USER)
                .build();
        var savedUser = repository.save(user);
        
        // Creamos adaptador rápido a UserDetails
        var userDetails = new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), java.util.Collections.emptyList());
        
        var jwtToken = jwtService.generateToken(userDetails);
        var refreshToken = jwtService.generateRefreshToken(userDetails);
        saveUserToken(savedUser, jwtToken);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();
        
        var userDetails = new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), java.util.Collections.emptyList());
        
        var jwtToken = jwtService.generateToken(userDetails);
        var refreshToken = jwtService.generateRefreshToken(userDetails);
        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    private void saveUserToken(UserEntity user, String jwtToken) {
        var token = TokenEntity.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(UserEntity user) {
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validUserTokens.isEmpty())
            return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }
}
