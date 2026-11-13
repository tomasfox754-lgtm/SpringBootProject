package com.logitrack.service;

import com.logitrack.dto.response.*;
import com.logitrack.model.TipoOperacion;

import java.util.List;

public interface ConsultaService {
    List<AuditoriaDtoResponse> auditorias(String usuario, TipoOperacion operacion);

    ResumenDtoResponse resumen();
}
