package com.logitrack.mapper;

import com.logitrack.dto.response.*;
import com.logitrack.model.Movimiento;

import org.springframework.stereotype.Component;

@Component
public class MovimientoMapper {
    public MovimientoDtoResponse entityToDto(Movimiento m) {
        var detalles =
                m.getDetalles().stream()
                        .map(
                                d ->
                                        new DetalleMovimientoDtoResponse(
                                                d.getProducto().getId(),
                                                d.getProducto().getNombre(),
                                                d.getCantidad()))
                        .toList();
        return new MovimientoDtoResponse(
                m.getId(),
                m.getFecha(),
                m.getTipo(),
                m.getUsuario().getUsername(),
                m.getOrigen() == null ? null : m.getOrigen().getId(),
                m.getDestino() == null ? null : m.getDestino().getId(),
                detalles);
    }
}
