package com.logitrack.controller;

import com.logitrack.dto.response.*;
import com.logitrack.model.TipoOperacion;
import com.logitrack.service.ConsultaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "consultas y reportes")
@SecurityRequirement(name = "bearerAuth")
public class ConsultaController {
    private final ConsultaService consultaService;

    @Operation(summary = "Consultar auditorias con filtros opcionales (ADMIN)")
    @GetMapping("/auditorias")
    public ResponseEntity<List<AuditoriaDtoResponse>> auditorias(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) TipoOperacion operacion) {
        return ResponseEntity.ok(consultaService.auditorias(usuario, operacion));
    }

    @Operation(
            summary = "Stock total por bodega y diez productos mas movidos",
            description =
                    "Suma unidades en todos los detalles; cada transferencia cuenta una vez por"
                        + " producto, nunca doble.")
    @GetMapping("/reportes/resumen")
    public ResponseEntity<ResumenDtoResponse> resumen() {
        return ResponseEntity.ok(consultaService.resumen());
    }
}
