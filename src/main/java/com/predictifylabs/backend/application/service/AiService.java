package com.predictifylabs.backend.application.service;

import com.predictifylabs.backend.application.ports.input.AiServiceUseCase;
import com.predictifylabs.backend.application.ports.output.AiGeneratorPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio de aplicación que orquesta la generación de contenido con IA.
 * Implementa el puerto de entrada y utiliza el puerto de salida para la generación.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiService implements AiServiceUseCase {

    private final AiGeneratorPort aiGeneratorPort;

    private static final String EVENT_DESCRIPTION_PROMPT_TEMPLATE = """
            Eres un asistente experto en comunidades tecnológicas y eventos de programación.
            Genera una descripción atractiva y profesional para un evento de tecnología con el siguiente contexto:
            
            %s
            
            La descripción debe:
            - Ser concisa (máximo 3 párrafos)
            - Incluir beneficios para los asistentes
            - Tener un tono profesional pero cercano
            - Estar en español
            
            Responde SOLO con la descripción, sin explicaciones adicionales.
            """;

    @Override
    public String generateEventDescription(String eventContext) {
        log.info("Generando descripción de evento con contexto: {}", eventContext);
        String prompt = String.format(EVENT_DESCRIPTION_PROMPT_TEMPLATE, eventContext);
        return aiGeneratorPort.generateText(prompt);
    }

    @Override
    public String generateText(String prompt) {
        log.info("Generando texto con prompt personalizado");
        return aiGeneratorPort.generateText(prompt);
    }
}
