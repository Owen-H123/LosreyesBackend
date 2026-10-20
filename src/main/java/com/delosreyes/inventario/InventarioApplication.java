package com.delosreyes.inventario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Punto de entrada principal del ERP "De los Reyes — Accesorios y Repuestos".
 *
 * Anotaciones:
 * - @SpringBootApplication: activa autoconfiguración, escaneo de componentes y configuración de Spring.
 * - @EnableCaching: activa el sistema de caché para endpoints de alto tráfico como productos y dashboard.
 * - @EnableJpaAuditing: permite el uso de @CreatedDate/@LastModifiedDate en entidades.
 * - @EnableAsync: habilita procesamiento asíncrono para auditoría y notificaciones futuras.
 */
@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableAsync
public class InventarioApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventarioApplication.class, args);
    }
}