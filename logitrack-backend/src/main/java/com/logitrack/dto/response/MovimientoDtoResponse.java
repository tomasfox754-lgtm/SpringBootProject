package com.logitrack.dto.response;

import com.logitrack.model.*;

import java.time.LocalDateTime;
import java.util.List;

public record MovimientoDtoResponse(
        Long id,
        LocalDateTime fecha,
        TipoMovimiento tipo,
        String usuario,
        Long origenId,
        Long destinoId,
        List<DetalleMovimientoDtoResponse> detalles) {}
