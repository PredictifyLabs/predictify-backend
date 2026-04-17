package com.predictifylabs.backend.infrastructure.security;

import com.predictifylabs.backend.infrastructure.adapters.input.rest.exception.ErrorCodes;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SecuredEndpointController())
                .addFilters(filter)
                .build();
    }

    @Test
    void malformedJwtShouldReturn401WithNormalizedErrorEnvelope() throws Exception {
        when(jwtService.extractUsername(anyString())).thenThrow(new MalformedJwtException("Token malformed"));

        mockMvc.perform(get("/secure/ping").header("Authorization", "Bearer malformed-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid JWT token"))
                .andExpect(jsonPath("$.path").value("/secure/ping"))
                .andExpect(jsonPath("$.code").value(ErrorCodes.AUTH_INVALID_TOKEN));
    }

    @Test
    void expiredJwtShouldReturn401WithExpiredTokenCode() throws Exception {
        when(jwtService.extractUsername(anyString()))
                .thenThrow(new ExpiredJwtException(null, null, "Token expired"));

        mockMvc.perform(get("/secure/ping").header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("JWT token has expired"))
                .andExpect(jsonPath("$.path").value("/secure/ping"))
                .andExpect(jsonPath("$.code").value(ErrorCodes.AUTH_TOKEN_EXPIRED));
    }

    @RestController
    @RequestMapping("/secure")
    static class SecuredEndpointController {
        @GetMapping("/ping")
        String ping() {
            return "pong";
        }
    }
}
