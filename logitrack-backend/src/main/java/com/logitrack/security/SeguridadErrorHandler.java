package com.logitrack.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitrack.exception.ErrorResponse;

import jakarta.servlet.http.*;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SeguridadErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException e)
            throws IOException {
        responder(req, res, 401, "Token ausente, invalido o expirado");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException e)
            throws IOException {
        responder(req, res, 403, "No tienes permisos para esta accion");
    }

    public void responder(
            HttpServletRequest req, HttpServletResponse res, int estado, String mensaje)
            throws IOException {
        res.setStatus(estado);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                res.getOutputStream(),
                new ErrorResponse(
                        LocalDateTime.now(), estado, mensaje, req.getRequestURI(), Map.of()));
    }
}
