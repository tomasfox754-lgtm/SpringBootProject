package com.logitrack.model;

import jakarta.persistence.*;

import lombok.*;

import java.util.Map;

@Entity
@Table(name = "bodega")
@Getter
@Setter
@NoArgsConstructor
public class Bodega extends EntidadAuditable {
    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String ubicacion;

    @Column(nullable = false)
    private Long capacidad;

    @Column(nullable = false, length = 100)
    private String encargado;

    @Override
    public Map<String, Object> valoresAuditoria() {
        return Map.of(
                "id",
                getId(),
                "nombre",
                nombre,
                "ubicacion",
                ubicacion,
                "capacidad",
                capacidad,
                "encargado",
                encargado);
    }
}
