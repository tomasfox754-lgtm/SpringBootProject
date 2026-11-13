package com.logitrack.repository;

import com.logitrack.model.*;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface BodegaRepository extends JpaRepository<Bodega, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bodega b where b.id = :id")
    Optional<Bodega> buscarParaActualizar(@Param("id") Long id);
}
