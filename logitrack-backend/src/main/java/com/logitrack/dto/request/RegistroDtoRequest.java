package com.logitrack.dto.request;

import jakarta.validation.constraints.*;

public record RegistroDtoRequest(
        @NotBlank
                @Pattern(
                        regexp = "[a-zA-Z0-9._-]{3,50}",
                        message = "Debe tener de 3 a 50 letras, numeros, punto, guion o guion bajo")
                String username,
        @NotBlank @Size(min = 8, max = 60) String password) {}
