package com.logitrack.dto.response;

import com.logitrack.model.*;

public record AuthDtoResponse(String token, String tipo, String username, Rol rol) {}
