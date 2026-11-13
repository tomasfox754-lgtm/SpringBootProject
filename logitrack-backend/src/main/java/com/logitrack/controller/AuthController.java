package com.logitrack.controller;

import com.logitrack.dto.request.*;
import com.logitrack.dto.response.*;
import com.logitrack.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "autenticacion")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "Registrar empleado; el rol siempre sera EMPLEADO")
    @PostMapping("/register")
    public ResponseEntity<UsuarioDtoResponse> registrar(
            @Valid @RequestBody RegistroDtoRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(dto));
    }

    @Operation(summary = "Obtener token JWT (duracion: una hora)")
    @PostMapping("/login")
    public ResponseEntity<AuthDtoResponse> login(@Valid @RequestBody LoginDtoRequest dto) {
        return ResponseEntity.ok(authService.login(dto));
    }
}
