package com.logitrack.controller;

import com.logitrack.dto.request.ProductoDtoRequest;
import com.logitrack.dto.response.*;
import com.logitrack.service.ProductoService;

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
@RequestMapping("/productos")
@RequiredArgsConstructor
@Validated
@Tag(name = "productos")
@SecurityRequirement(name = "bearerAuth")
public class ProductoController {
    private final ProductoService productoService;

    @Operation(summary = "Registrar producto (ADMIN)")
    @PostMapping
    public ResponseEntity<ProductoDtoResponse> guardar(@Valid @RequestBody ProductoDtoRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.guardar(dto));
    }

    @Operation(summary = "Listar productos")
    @GetMapping
    public ResponseEntity<List<ProductoDtoResponse>> listar() {
        return ResponseEntity.ok(productoService.listar());
    }

    @Operation(summary = "Buscar producto por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProductoDtoResponse> buscarId(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(productoService.buscarId(id));
    }

    @Operation(summary = "Actualizar producto (ADMIN)")
    @PutMapping("/{id}")
    public ResponseEntity<ProductoDtoResponse> actualizar(
            @Valid @RequestBody ProductoDtoRequest dto, @PathVariable @Positive Long id) {
        return ResponseEntity.ok(productoService.actualizar(dto, id));
    }

    @Operation(summary = "Eliminar producto sin relaciones (ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Consultar productos con stock global menor a 10")
    @GetMapping("/stock-bajo")
    public ResponseEntity<List<ProductoDtoResponse>> stockBajo() {
        return ResponseEntity.ok(productoService.stockBajo());
    }
}
