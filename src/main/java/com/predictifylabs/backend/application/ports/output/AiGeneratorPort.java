package com.predictifylabs.backend.application.ports.output;

/**
 * Puerto de salida para generación de texto con IA.
 * Define el contrato que debe cumplir cualquier adaptador de IA (Gemini, OpenAI, etc.)
 */
public interface AiGeneratorPort {

    /**
     * Genera texto basado en un prompt dado.
     *
     * @param prompt El texto de entrada para la IA
     * @return El texto generado por la IA
     */
    String generateText(String prompt);
}
