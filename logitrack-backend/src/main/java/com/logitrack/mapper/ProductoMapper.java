package com.logitrack.mapper;

import com.logitrack.dto.request.ProductoDtoRequest;
import com.logitrack.dto.response.ProductoDtoResponse;
import com.logitrack.model.Producto;

import org.springframework.stereotype.Component;

@Component
public class ProductoMapper {
    public ProductoDtoResponse entityToDto(Producto p) {
        return new ProductoDtoResponse(
                p.getId(), p.getNombre(), p.getCategoria(), p.getStock(), p.getPrecio());
    }

    public Producto DtoToEntity(ProductoDtoRequest dto) {
        Producto p = new Producto();
        updateEntityFromDto(p, dto);
        return p;
    }

    public void updateEntityFromDto(Producto p, ProductoDtoRequest dto) {
        p.setNombre(dto.nombre());
        p.setCategoria(dto.categoria());
        p.setPrecio(dto.precio());
    }
}
