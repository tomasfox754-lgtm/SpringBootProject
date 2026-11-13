package com.logitrack.model;

import jakarta.persistence.*;

import lombok.*;

import java.util.Map;

@Entity
@Table(
        name = "inventario",
        uniqueConstraints = @UniqueConstraint(columnNames = {"bodega_id", "producto_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Inventario extends EntidadAuditable {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bodega_id")
    private Bodega bodega;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(nullable = false)
    private Long cantidad = 0L;

    @Override
    public Map<String, Object> valoresAuditoria() {
        return Map.of(
                "id",
                getId(),
                "bodegaId",
                bodega.getId(),
                "productoId",
                producto.getId(),
                "cantidad",
                cantidad);
    }
}
