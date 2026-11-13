package com.logitrack.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        LocalDateTime fecha,
        int estado,
        String mensaje,
        String ruta,
        Map<String, String> errores) {}
