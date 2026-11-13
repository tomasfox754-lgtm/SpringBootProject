package com.logitrack.repository;

import com.logitrack.model.*;

import org.springframework.data.jpa.repository.*;

import java.util.*;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);
}
