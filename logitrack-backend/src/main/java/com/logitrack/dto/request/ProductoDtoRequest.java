package com.logitrack.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductoDtoRequest(
        @NotBlank @Size(max = 80) String nombre,
        @NotBlank @Size(max = 60) String categoria,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal precio) {}
