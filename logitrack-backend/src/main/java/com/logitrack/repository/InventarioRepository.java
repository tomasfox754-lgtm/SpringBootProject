package com.logitrack.repository;

import com.logitrack.model.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {
    Optional<Inventario> findByBodegaIdAndProductoId(Long bodegaId, Long productoId);

    List<Inventario> findByBodegaId(Long bodegaId);

    boolean existsByBodegaId(Long bodegaId);

    boolean existsByProductoId(Long productoId);

    @Query("select coalesce(sum(i.cantidad), 0) from Inventario i where i.bodega.id = :id")
    Long stockEnBodega(@Param("id") Long id);

    @Query(
            "select new com.logitrack.dto.response.StockBodegaDtoResponse(b.id, b.nombre,"
                + " coalesce(sum(i.cantidad), 0L)) from Bodega b left join Inventario i on"
                + " i.bodega.id = b.id group by b.id, b.nombre order by b.id")
    List<com.logitrack.dto.response.StockBodegaDtoResponse> resumenBodegas();
}
