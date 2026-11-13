package com.logitrack.dto.request;

import jakarta.validation.constraints.*;

public record LoginDtoRequest(
        @NotBlank @Size(max = 50) String username, @NotBlank @Size(max = 60) String password) {}
