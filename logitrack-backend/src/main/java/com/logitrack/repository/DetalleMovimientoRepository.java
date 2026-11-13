package com.logitrack.repository;

import com.logitrack.model.*;

import org.springframework.data.jpa.repository.*;

import java.util.*;

public interface DetalleMovimientoRepository extends JpaRepository<DetalleMovimiento, Long> {
    boolean existsByProductoId(Long id);

    @Query(
            "select new com.logitrack.dto.response.ProductoMovidoDtoResponse(d.producto.id,"
                + " d.producto.nombre, sum(d.cantidad)) from DetalleMovimiento d group by"
                + " d.producto.id, d.producto.nombre order by sum(d.cantidad) desc, d.producto.id"
                + " asc")
    List<com.logitrack.dto.response.ProductoMovidoDtoResponse> productosMasMovidos(
            org.springframework.data.domain.Pageable pagina);
}
