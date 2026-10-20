package com.delosreyes.inventario.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * OpenApiConfig — Configuración de Swagger/OpenAPI 3.
 *
 * QUÉ HACE:
 *   Genera documentación interactiva en /swagger-ui.html.
 *   Permite al equipo frontend probar endpoints directamente con JWT.
 *
 * ACCESO:
 *   http://localhost:8080/api/v1/swagger-ui.html
 *   http://localhost:8080/api/v1/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("De los Reyes ERP — API REST")
                .description("""
                    Sistema ERP automotriz para gestión de repuestos y accesorios.
                    Especializado en Peugeot, Citroën y Renault.
                    
                    ## Autenticación
                    Usar endpoint `/api/v1/auth/login` para obtener el JWT.
                    Incluir en header: `Authorization: Bearer <token>`
                    
                    ## Roles
                    - **ADMIN**: Acceso total al sistema
                    - **WORKER**: Acceso a consultas y ventas
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("De los Reyes — Soporte Técnico")
                    .email("soporte@delosreyes.com"))
                .license(new License().name("Privado").url("#")))
            .servers(List.of(
                new Server().url("http://localhost:8080/api/v1").description("Desarrollo"),
                new Server().url("https://api.delosreyes.com/v1").description("Producción")
            ))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Ingrese el token JWT obtenido del endpoint /auth/login")));
    }
}