package com.predictifylabs.backend.infrastructure.adapters.output.ai;

import com.predictifylabs.backend.application.ports.output.AiGeneratorPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Infrastructure adapter that implements communication with the Google Gemini API.
 */
@Component
@Slf4j
public class GeminiAdapter implements AiGeneratorPort {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    public GeminiAdapter(
            @Value("${application.ai.gemini.api-key:}") String apiKey,
            @Value("${application.ai.gemini.model:gemini-1.5-flash}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(GEMINI_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String generateText(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key not configured. Returning mock response.");
            return getMockResponse(prompt);
        }

        try {
            String url = String.format("/models/%s:generateContent?key=%s", model, apiKey);

            var requestBody = buildRequestBody(prompt);

            GeminiResponse response = restClient.post()
                    .uri(url)
                    .body(requestBody)
                    .retrieve()
                    .body(GeminiResponse.class);

            return extractTextFromResponse(response);

        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating text with AI: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 1024
                )
        );
    }

    private String extractTextFromResponse(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new RuntimeException("Empty response from Gemini API");
        }

        var candidate = response.candidates().get(0);
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            throw new RuntimeException("Empty content in Gemini response");
        }

        return candidate.content().parts().get(0).text();
    }

    private String getMockResponse(String prompt) {
        return """
                [DEVELOPMENT MODE - API Key not configured]
                
                This is a simulated response for the received prompt.
                To enable Gemini, configure the property:
                application.ai.gemini.api-key=YOUR_API_KEY
                
                Received prompt: %s
                """.formatted(prompt.length() > 100 ? prompt.substring(0, 100) + "..." : prompt);
    }

    // Records for deserializing Gemini response
    record GeminiResponse(List<Candidate> candidates) {}
    record Candidate(Content content) {}
    record Content(List<Part> parts) {}
    record Part(String text) {}
}
