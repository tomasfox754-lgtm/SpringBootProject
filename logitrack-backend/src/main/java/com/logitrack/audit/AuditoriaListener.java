package com.logitrack.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitrack.model.EntidadAuditable;

import jakarta.persistence.*;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// Hibernate obtiene este listener de Spring, por eso podemos inyectar sus dependencias.
@Component
@RequiredArgsConstructor
public class AuditoriaListener {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @PostLoad
    public void recordar(EntidadAuditable entidad) {
        entidad.setValoresAnteriores(json(entidad));
    }

    @PostPersist
    public void insert(EntidadAuditable entidad) {
        registrar(entidad, "INSERT", null, json(entidad));
    }

    @PostUpdate
    public void update(EntidadAuditable entidad) {
        registrar(entidad, "UPDATE", entidad.getValoresAnteriores(), json(entidad));
    }

    @PreRemove
    public void delete(EntidadAuditable entidad) {
        registrar(entidad, "DELETE", json(entidad), null);
    }

    private String json(EntidadAuditable entidad) {
        try {
            return objectMapper.writeValueAsString(entidad.valoresAuditoria());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No fue posible auditar el cambio", e);
        }
    }

    private void registrar(
            EntidadAuditable entidad, String operacion, String antes, String despues) {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        String usuario = autenticacion == null ? "SISTEMA" : autenticacion.getName();
        // JDBC evita ejecutar operaciones de EntityManager dentro de callbacks JPA.
        // Usa la misma conexion y transaccion: si el movimiento falla, la auditoria tambien se
        // revierte.
        jdbcTemplate.update(
                "INSERT INTO auditoria (operacion, fecha, usuario, entidad, entidad_id,"
                    + " valores_anteriores, valores_nuevos) VALUES (?, ?, ?, ?, ?, ?, ?)",
                operacion,
                LocalDateTime.now(),
                usuario,
                entidad.getClass().getSimpleName(),
                entidad.getId(),
                antes,
                despues);
        entidad.setValoresAnteriores(despues);
    }
}
