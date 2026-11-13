package com.logitrack.dto.request;

import jakarta.validation.constraints.*;

public record DetalleMovimientoDtoRequest(
        @NotNull @Positive Long productoId, @NotNull @Min(1) @Max(1000000) Integer cantidad) {}
