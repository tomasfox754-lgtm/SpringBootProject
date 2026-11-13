package com.logitrack.controller;

import com.logitrack.dto.request.MovimientoDtoRequest;
import com.logitrack.dto.response.MovimientoDtoResponse;
import com.logitrack.service.MovimientoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/movimientos")
@RequiredArgsConstructor
@Validated
@Tag(name = "movimientos")
@SecurityRequirement(name = "bearerAuth")
public class MovimientoController {
    private final MovimientoService movimientoService;

    @Operation(
            summary = "Registrar entrada, salida o transferencia",
            description =
                    "El usuario se toma del token. ENTRADA: solo destinoId. SALIDA: solo origenId."
                        + " TRANSFERENCIA: ambos diferentes. No se editan ni eliminan movimientos"
                        + " historicos.")
    @PostMapping
    public ResponseEntity<MovimientoDtoResponse> guardar(
            @Valid @RequestBody MovimientoDtoRequest dto, Principal usuario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(movimientoService.guardar(dto, usuario.getName()));
    }

    @Operation(
            summary = "Listar movimientos; rango opcional e inclusivo BETWEEN",
            description =
                    "Enviar desde y hasta juntos en formato yyyy-MM-dd'T'HH:mm:ss. Ejemplo:"
                        + " 2026-11-01T00:00:00")
    @GetMapping
    public ResponseEntity<List<MovimientoDtoResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime hasta) {
        return ResponseEntity.ok(movimientoService.listar(desde, hasta));
    }

    @Operation(summary = "Consultar movimiento por ID")
    @GetMapping("/{id}")
    public ResponseEntity<MovimientoDtoResponse> buscar(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(movimientoService.buscarId(id));
    }
}
