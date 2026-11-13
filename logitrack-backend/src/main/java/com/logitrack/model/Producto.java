package com.logitrack.model;

import jakarta.persistence.*;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
public class Producto extends EntidadAuditable {
    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 60)
    private String categoria;

    @Column(nullable = false)
    private Long stock = 0L;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Override
    public Map<String, Object> valoresAuditoria() {
        return Map.of(
                "id",
                getId(),
                "nombre",
                nombre,
                "categoria",
                categoria,
                "stock",
                stock,
                "precio",
                precio);
    }
}
