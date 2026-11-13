package com.logitrack.model;

import com.logitrack.audit.AuditoriaListener;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@MappedSuperclass
@EntityListeners(AuditoriaListener.class)
@Getter
@Setter
public abstract class EntidadAuditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient private String valoresAnteriores;

    public abstract Map<String, Object> valoresAuditoria();
}
