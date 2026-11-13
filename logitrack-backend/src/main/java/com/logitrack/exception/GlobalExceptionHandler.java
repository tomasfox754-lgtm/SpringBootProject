package com.logitrack.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<ErrorResponse> error(
            int codigo, String mensaje, HttpServletRequest r, Map<String, String> detalles) {
        return ResponseEntity.status(codigo)
                .body(
                        new ErrorResponse(
                                LocalDateTime.now(), codigo, mensaje, r.getRequestURI(), detalles));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(Exception e, HttpServletRequest r) {
        return error(404, e.getMessage(), r, Map.of());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> regla(Exception e, HttpServletRequest r) {
        return error(400, e.getMessage(), r, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(
            MethodArgumentNotValidException e, HttpServletRequest r) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult()
                .getFieldErrors()
                .forEach(f -> campos.put(f.getField(), f.getDefaultMessage()));
        return error(400, "Revisa los campos enviados", r, campos);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        ConstraintViolationException.class
    })
    public ResponseEntity<ErrorResponse> formato(Exception e, HttpServletRequest r) {
        return error(400, "Formato invalido: revisa tipos, fechas y valores del enum", r, Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> autenticacion(Exception e, HttpServletRequest r) {
        return error(401, "Credenciales incorrectas", r, Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> acceso(Exception e, HttpServletRequest r) {
        return error(403, "No tienes permisos para esta accion", r, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(Exception e, HttpServletRequest r) {
        return error(400, "El registro esta duplicado o tiene datos relacionados", r, Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> ruta(Exception e, HttpServletRequest r) {
        return error(404, "Ruta no encontrada", r, Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodo(Exception e, HttpServletRequest r) {
        return error(405, "Metodo HTTP no permitido", r, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception e, HttpServletRequest r) {
        log.error("Error inesperado en {}", r.getRequestURI(), e);
        return error(500, "Error interno del servidor", r, Map.of());
    }
}
