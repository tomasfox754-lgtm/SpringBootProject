package com.logitrack.config;

import com.logitrack.security.*;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    private final SeguridadErrorHandler errorHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // El filtro solo se ejecuta dentro de Spring Security, no dos veces como filtro del servlet.
    @Bean
    public FilterRegistrationBean<JwtFilter> registroFiltro(JwtFilter filtro) {
        FilterRegistrationBean<JwtFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(errorHandler)
                                        .accessDeniedHandler(errorHandler))
                .authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                "/",
                                                "/index.html",
                                                "/styles.css",
                                                "/app.js",
                                                "/favicon.ico",
                                                "/error",
                                                "/swagger-ui.html",
                                                "/swagger-ui/**",
                                                "/v3/api-docs/**")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.POST, "/auth/login", "/auth/register")
                                        .permitAll()
                                        .requestMatchers("/auditorias/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/bodegas/**",
                                                "/productos/**",
                                                "/movimientos/**",
                                                "/reportes/**")
                                        .hasAnyRole("ADMIN", "EMPLEADO")
                                        .requestMatchers("/bodegas/**", "/productos/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers(HttpMethod.POST, "/movimientos")
                                        .hasAnyRole("ADMIN", "EMPLEADO")
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // Live Server y Spring Boot se ejecutan en puertos distintos.
    // Estos son los unicos origenes locales autorizados para el frontend.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(
                List.of(
                        "http://127.0.0.1:5500", "http://localhost:5500",
                        "http://127.0.0.1:5501", "http://localhost:5501"));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
