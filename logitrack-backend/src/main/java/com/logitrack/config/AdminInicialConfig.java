package com.logitrack.config;

import com.logitrack.model.*;
import com.logitrack.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminInicialConfig {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner crearAdmin(
            @Value("${app.admin.username:}") String username,
            @Value("${app.admin.password:}") String password) {
        return args -> {
            if (!username.isBlank() && !usuarioRepository.existsByUsername(username)) {
                if (!username.matches("[a-zA-Z0-9._-]{3,50}"))
                    throw new IllegalArgumentException("ADMIN_USERNAME invalido");
                if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
                        || password.length() < 8
                        || password.length() > 60)
                    throw new IllegalArgumentException(
                            "ADMIN_PASSWORD requiere entre 8 y 60 caracteres");
                Usuario usuario = new Usuario();
                usuario.setUsername(username);
                usuario.setPassword(passwordEncoder.encode(password));
                usuario.setRol(Rol.ADMIN);
                usuarioRepository.save(usuario);
            }
        };
    }
}
