package com.university.ums.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        // example values pre-fill the Swagger UI form
        @Schema(example = "admin") @NotBlank String username,
        @Schema(example = "admin123") @NotBlank String password
) {
}
