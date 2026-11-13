package com.logitrack.model;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "movimiento")
@Getter
@Setter
@NoArgsConstructor
public class Movimiento extends EntidadAuditable {
    @Column(nullable = false)
    private LocalDateTime fecha;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origen_id")
    private Bodega origen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destino_id")
    private Bodega destino;

    @OneToMany(mappedBy = "movimiento", cascade = CascadeType.ALL)
    private List<DetalleMovimiento> detalles = new ArrayList<>();

    @Override
    public Map<String, Object> valoresAuditoria() {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("id", getId());
        valores.put("fecha", fecha.toString());
        valores.put("tipo", tipo.name());
        valores.put("usuario", usuario.getUsername());
        valores.put("origenId", origen == null ? null : origen.getId());
        valores.put("destinoId", destino == null ? null : destino.getId());
        valores.put(
                "detalles",
                detalles.stream()
                        .map(
                                d ->
                                        Map.of(
                                                "productoId",
                                                d.getProducto().getId(),
                                                "cantidad",
                                                d.getCantidad()))
                        .toList());
        return valores;
    }
}
