package com.predictifylabs.backend.application.ports.input;

/**
 * Puerto de entrada que define los casos de uso del servicio de IA.
 */
public interface AiServiceUseCase {

    /**
     * Genera una descripción para un evento basado en el contexto proporcionado.
     *
     * @param eventContext Contexto del evento (título, tipo, tecnología, etc.)
     * @return Descripción generada por la IA
     */
    String generateEventDescription(String eventContext);

    /**
     * Genera texto libre basado en un prompt personalizado.
     *
     * @param prompt El prompt a enviar a la IA
     * @return Texto generado
     */
    String generateText(String prompt);
}
