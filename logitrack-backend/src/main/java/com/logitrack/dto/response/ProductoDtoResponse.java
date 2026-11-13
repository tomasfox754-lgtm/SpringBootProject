package com.logitrack.dto.response;

import com.logitrack.model.*;

import java.math.BigDecimal;

public record ProductoDtoResponse(
        Long id, String nombre, String categoria, Long stock, BigDecimal precio) {}
