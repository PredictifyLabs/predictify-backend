package com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenerateTextRequest {

    @NotBlank(message = "El prompt no puede estar vacío")
    @Size(min = 10, max = 5000, message = "El prompt debe tener entre 10 y 5000 caracteres")
    private String prompt;
}
