package com.predictifylabs.backend.infrastructure.adapters.input.rest.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ExceptionThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void authTokenInvalidExceptionShouldMapDeterministicallyTo401() throws Exception {
        mockMvc.perform(get("/test/invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Malformed token"))
                .andExpect(jsonPath("$.path").value("/test/invalid-token"))
                .andExpect(jsonPath("$.code").value(ErrorCodes.AUTH_INVALID_TOKEN));
    }

    @Test
    void resourceNotFoundExceptionShouldMapDeterministicallyTo404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Event not found"))
                .andExpect(jsonPath("$.path").value("/test/not-found"))
                .andExpect(jsonPath("$.code").value(ErrorCodes.RESOURCE_NOT_FOUND));
    }

    @Test
    void unknownExceptionsShouldRemain500WithInternalErrorCode() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
                .andExpect(jsonPath("$.path").value("/test/unexpected"))
                .andExpect(jsonPath("$.code").value(ErrorCodes.INTERNAL_ERROR));
    }

    @RestController
    @RequestMapping("/test")
    static class ExceptionThrowingController {

        @GetMapping("/invalid-token")
        String invalidToken() {
            throw new AuthTokenInvalidException("Malformed token");
        }

        @GetMapping("/not-found")
        String notFound() {
            throw new ResourceNotFoundException("Event not found");
        }

        @GetMapping("/unexpected")
        String unexpected() {
            throw new RuntimeException("Boom");
        }
    }
}
