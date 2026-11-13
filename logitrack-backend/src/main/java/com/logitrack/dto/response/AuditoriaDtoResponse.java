package com.logitrack.dto.response;

import com.logitrack.model.*;

import java.time.LocalDateTime;

public record AuditoriaDtoResponse(
        Long id,
        TipoOperacion operacion,
        LocalDateTime fecha,
        String usuario,
        String entidad,
        Long entidadId,
        String valoresAnteriores,
        String valoresNuevos) {}
