package com.predictifylabs.backend.application.service;

import com.predictifylabs.backend.application.ports.input.AiServiceUseCase;
import com.predictifylabs.backend.application.ports.output.AiGeneratorPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application service that orchestrates AI content generation.
 * Implements the input port and uses the output port for generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiService implements AiServiceUseCase {

    private final AiGeneratorPort aiGeneratorPort;

    private static final String EVENT_DESCRIPTION_PROMPT_TEMPLATE = """
            You are an expert assistant in tech communities and programming events.
            Generate an attractive and professional description for a technology event with the following context:
            
            %s
            
            The description should:
            - Be concise (maximum 3 paragraphs)
            - Include benefits for attendees
            - Have a professional yet approachable tone
            - Be in English
            
            Respond ONLY with the description, without additional explanations.
            """;

    @Override
    public String generateEventDescription(String eventContext) {
        log.info("Generating event description with context: {}", eventContext);
        String prompt = String.format(EVENT_DESCRIPTION_PROMPT_TEMPLATE, eventContext);
        return aiGeneratorPort.generateText(prompt);
    }

    @Override
    public String generateText(String prompt) {
        log.info("Generating text with custom prompt");
        return aiGeneratorPort.generateText(prompt);
    }
}
