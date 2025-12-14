package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenerateTextResponse {

    private String generatedText;
    private String model;
    private LocalDateTime generatedAt;
}
