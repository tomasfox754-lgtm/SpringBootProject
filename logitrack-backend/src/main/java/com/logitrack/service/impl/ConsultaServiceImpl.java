package com.logitrack.service.impl;

import com.logitrack.dto.response.*;
import com.logitrack.model.TipoOperacion;
import com.logitrack.repository.*;
import com.logitrack.service.ConsultaService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultaServiceImpl implements ConsultaService {
    private final AuditoriaRepository auditoriaRepository;
    private final InventarioRepository inventarioRepository;
    private final DetalleMovimientoRepository detalleMovimientoRepository;

    @Override
    public List<AuditoriaDtoResponse> auditorias(String usuario, TipoOperacion operacion) {
        return auditoriaRepository.filtrar(usuario, operacion).stream()
                .map(
                        a ->
                                new AuditoriaDtoResponse(
                                        a.getId(),
                                        a.getOperacion(),
                                        a.getFecha(),
                                        a.getUsuario(),
                                        a.getEntidad(),
                                        a.getEntidadId(),
                                        a.getValoresAnteriores(),
                                        a.getValoresNuevos()))
                .toList();
    }

    @Override
    public ResumenDtoResponse resumen() {
        return new ResumenDtoResponse(
                inventarioRepository.resumenBodegas(),
                detalleMovimientoRepository.productosMasMovidos(PageRequest.of(0, 10)));
    }
}
