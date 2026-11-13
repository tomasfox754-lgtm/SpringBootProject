package com.logitrack.service.impl;

import com.logitrack.dto.request.*;
import com.logitrack.dto.response.*;
import com.logitrack.exception.ReglaNegocioException;
import com.logitrack.model.*;
import com.logitrack.repository.UsuarioRepository;
import com.logitrack.security.JwtService;
import com.logitrack.service.AuthService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    @Transactional
    public UsuarioDtoResponse registrar(RegistroDtoRequest dto) {
        if (dto.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new ReglaNegocioException("La contrasena supera el limite de 72 bytes de BCrypt");
        if (usuarioRepository.existsByUsername(dto.username()))
            throw new ReglaNegocioException("El nombre de usuario ya existe");
        Usuario usuario = new Usuario();
        usuario.setUsername(dto.username());
        usuario.setPassword(passwordEncoder.encode(dto.password()));
        usuario.setRol(Rol.EMPLEADO); // Nunca aceptar un rol enviado por registro publico.
        usuarioRepository.save(usuario);
        return new UsuarioDtoResponse(usuario.getId(), usuario.getUsername(), usuario.getRol());
    }

    @Override
    public AuthDtoResponse login(LoginDtoRequest dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.username(), dto.password()));
        Usuario usuario =
                usuarioRepository
                        .findByUsername(dto.username())
                        .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));
        return new AuthDtoResponse(
                jwtService.generarToken(usuario),
                "Bearer",
                usuario.getUsername(),
                usuario.getRol());
    }
}
