package com.logitrack.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;

import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("LogiTrack API")
                                .version("1.0")
                                .description(
                                        "Gestion de bodegas, inventario y auditoria. Login y"
                                            + " registro son publicos; el registro crea EMPLEADO."
                                            + " ADMIN gestiona catalogos y consulta auditorias. Los"
                                            + " movimientos son inmutables."))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "bearerAuth",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")));
    }
}
