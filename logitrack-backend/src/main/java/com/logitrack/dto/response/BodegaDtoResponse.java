package com.logitrack.dto.response;

import com.logitrack.model.*;

public record BodegaDtoResponse(
        Long id, String nombre, String ubicacion, Long capacidad, String encargado) {}
