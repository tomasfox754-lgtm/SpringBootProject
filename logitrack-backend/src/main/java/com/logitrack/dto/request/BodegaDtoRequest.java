package com.logitrack.dto.request;

import jakarta.validation.constraints.*;

public record BodegaDtoRequest(
        @NotBlank @Size(max = 80) String nombre,
        @NotBlank @Size(max = 150) String ubicacion,
        @NotNull @Min(1) @Max(1000000000) Long capacidad,
        @NotBlank @Size(max = 100) String encargado) {}
