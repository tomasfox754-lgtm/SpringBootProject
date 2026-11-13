package com.logitrack.repository;

import com.logitrack.model.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    @Query(
            "select a from Auditoria a where (:usuario is null or a.usuario = :usuario) and"
                + " (:operacion is null or a.operacion = :operacion) order by a.fecha desc, a.id"
                + " desc")
    List<Auditoria> filtrar(
            @Param("usuario") String usuario, @Param("operacion") TipoOperacion operacion);
}
