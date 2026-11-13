package com.logitrack.controller;

import com.logitrack.dto.request.BodegaDtoRequest;
import com.logitrack.dto.response.*;
import com.logitrack.service.BodegaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bodegas")
@RequiredArgsConstructor
@Validated
@Tag(name = "bodegas")
@SecurityRequirement(name = "bearerAuth")
public class BodegaController {
    private final BodegaService bodegaService;

    @Operation(summary = "Registrar bodega (ADMIN)")
    @PostMapping
    public ResponseEntity<BodegaDtoResponse> guardar(@Valid @RequestBody BodegaDtoRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bodegaService.guardar(dto));
    }

    @Operation(summary = "Listar bodegas")
    @GetMapping
    public ResponseEntity<List<BodegaDtoResponse>> listar() {
        return ResponseEntity.ok(bodegaService.listar());
    }

    @Operation(summary = "Buscar bodega por ID")
    @GetMapping("/{id}")
    public ResponseEntity<BodegaDtoResponse> buscarId(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(bodegaService.buscarId(id));
    }

    @Operation(summary = "Actualizar bodega (ADMIN)")
    @PutMapping("/{id}")
    public ResponseEntity<BodegaDtoResponse> actualizar(
            @Valid @RequestBody BodegaDtoRequest dto, @PathVariable @Positive Long id) {
        return ResponseEntity.ok(bodegaService.actualizar(dto, id));
    }

    @Operation(summary = "Eliminar bodega sin relaciones (ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        bodegaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Consultar existencias por producto en la bodega")
    @GetMapping("/{id}/inventario")
    public ResponseEntity<List<InventarioDtoResponse>> inventario(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(bodegaService.inventario(id));
    }
}
