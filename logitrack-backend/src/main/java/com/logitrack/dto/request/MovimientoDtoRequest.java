package com.logitrack.dto.request;

import com.logitrack.model.TipoMovimiento;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record MovimientoDtoRequest(
        @NotNull TipoMovimiento tipo,
        @Positive Long origenId,
        @Positive Long destinoId,
        @NotEmpty @Size(max = 100) List<@Valid @NotNull DetalleMovimientoDtoRequest> detalles) {}
