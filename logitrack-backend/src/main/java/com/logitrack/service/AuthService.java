package com.logitrack.service;

import com.logitrack.dto.request.*;
import com.logitrack.dto.response.*;

public interface AuthService {
    UsuarioDtoResponse registrar(RegistroDtoRequest dto);

    AuthDtoResponse login(LoginDtoRequest dto);
}
