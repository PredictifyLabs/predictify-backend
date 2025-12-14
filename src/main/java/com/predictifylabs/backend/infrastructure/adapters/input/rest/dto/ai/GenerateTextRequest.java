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

    @NotBlank(message = "Prompt cannot be empty")
    @Size(min = 10, max = 5000, message = "Prompt must be between 10 and 5000 characters")
    private String prompt;
}
