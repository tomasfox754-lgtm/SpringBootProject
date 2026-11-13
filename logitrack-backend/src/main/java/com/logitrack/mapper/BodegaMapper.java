package com.logitrack.mapper;

import com.logitrack.dto.request.BodegaDtoRequest;
import com.logitrack.dto.response.BodegaDtoResponse;
import com.logitrack.model.Bodega;

import org.springframework.stereotype.Component;

@Component
public class BodegaMapper {
    public BodegaDtoResponse entityToDto(Bodega b) {
        return new BodegaDtoResponse(
                b.getId(), b.getNombre(), b.getUbicacion(), b.getCapacidad(), b.getEncargado());
    }

    public Bodega DtoToEntity(BodegaDtoRequest dto) {
        Bodega b = new Bodega();
        updateEntityFromDto(b, dto);
        return b;
    }

    public void updateEntityFromDto(Bodega b, BodegaDtoRequest dto) {
        b.setNombre(dto.nombre());
        b.setUbicacion(dto.ubicacion());
        b.setCapacidad(dto.capacidad());
        b.setEncargado(dto.encargado());
    }
}
