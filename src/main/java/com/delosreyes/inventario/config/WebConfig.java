package com.delosreyes.inventario.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebConfig — Sirve los archivos subidos (imágenes de productos) como
 * recursos estáticos.
 *
 * Un archivo guardado físicamente en {app.upload.dir}/products/xxxx.jpg
 * queda accesible en la URL /uploads/products/xxxx.jpg — bajo el mismo
 * context-path /api/v1 que el resto de la API (Spring lo aplica también a
 * los resource handlers), así que el frontend arma la URL completa igual
 * que cualquier otro endpoint: {IP configurada en Conexión}:8080/api/v1/uploads/...
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + uploadDir.replaceAll("/$", "") + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}