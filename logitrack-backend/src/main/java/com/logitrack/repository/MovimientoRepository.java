package com.logitrack.repository;

import com.logitrack.model.*;

import org.springframework.data.jpa.repository.*;

import java.time.LocalDateTime;
import java.util.*;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {
    List<Movimiento> findByFechaBetweenOrderByFechaDesc(LocalDateTime desde, LocalDateTime hasta);

    List<Movimiento> findAllByOrderByFechaDesc();

    boolean existsByOrigenIdOrDestinoId(Long origenId, Long destinoId);
}
