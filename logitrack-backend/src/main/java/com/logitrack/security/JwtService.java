package com.logitrack.security;

import com.logitrack.model.Usuario;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

@Service
public class JwtService {
    private final SecretKey key;
    private final long duracion;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:3600000}") long duracion) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalArgumentException("JWT_SECRET requiere al menos 32 bytes");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.duracion = duracion;
    }

    public String generarToken(Usuario usuario) {
        return Jwts.builder()
                .subject(usuario.getUsername())
                .issuer("logitrack")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + duracion))
                .signWith(key)
                .compact();
    }

    public String obtenerUsername(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer("logitrack")
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
