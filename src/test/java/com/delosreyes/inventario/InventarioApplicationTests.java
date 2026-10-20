package com.delosreyes.inventario;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * InventarioApplicationTests — Test de arranque del contexto Spring.
 *
 * @ActiveProfiles("test"):
 *   Carga application-test.properties en lugar de application.properties.
 *   Usa H2 en memoria → no necesita Docker ni PostgreSQL corriendo.
 *   Flyway desactivado → no ejecuta migraciones en tests.
 *
 * SIN esta anotación:
 *   Spring intentaría conectar a PostgreSQL real → falla si Docker no está listo.
 */
@SpringBootTest
@ActiveProfiles("test")
class InventarioApplicationTests {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring arranca correctamente
        // No necesita BD real gracias al perfil "test"
    }
}