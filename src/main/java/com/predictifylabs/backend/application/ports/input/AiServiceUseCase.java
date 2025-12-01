package com.predictifylabs.backend.application.ports.input;

/**
 * Input port that defines the AI service use cases.
 */
public interface AiServiceUseCase {

    /**
     * Generates a description for an event based on the provided context.
     *
     * @param eventContext Event context (title, type, technology, etc.)
     * @return AI-generated description
     */
    String generateEventDescription(String eventContext);

    /**
     * Generates free text based on a custom prompt.
     *
     * @param prompt The prompt to send to the AI
     * @return Generated text
     */
    String generateText(String prompt);
}
