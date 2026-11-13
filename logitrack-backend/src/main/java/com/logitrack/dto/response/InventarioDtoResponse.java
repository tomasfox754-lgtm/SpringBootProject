package com.logitrack.dto.response;

import com.logitrack.model.*;

public record InventarioDtoResponse(
        Long bodegaId, Long productoId, String producto, Long cantidad) {}
