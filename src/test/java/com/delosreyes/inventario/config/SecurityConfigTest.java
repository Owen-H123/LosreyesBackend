package com.delosreyes.inventario.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la respuesta de seguridad cuando no hay sesión válida.
 *
 * El frontend solo renueva el token cuando recibe 401. Si el backend
 * respondiera 403, la sesión vencida nunca se renovaría y las listas
 * aparecerían vacías.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/categories/active"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenInvalidoResponde401() throws Exception {
        mockMvc.perform(get("/categories/active")
                .header("Authorization", "Bearer token-vencido-o-invalido"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void losEndpointsTemporalesDeContrasenaYaNoExisten() throws Exception {
        // /auth/encode y /auth/debug-password eran públicos y exponían datos de la
        // contraseña del admin. Se eliminaron: ahora responden 404 (ruta inexistente).
        mockMvc.perform(get("/auth/encode")).andExpect(status().isNotFound());
        mockMvc.perform(get("/auth/debug-password")).andExpect(status().isNotFound());
    }

    @Test
    void corsAceptaElFrontendDesdeOtraPcDeLaRed() throws Exception {
        mockMvc.perform(options("/categories/active")
                .header("Origin", "http://192.168.1.50:3000")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://192.168.1.50:3000"));
    }

    @Test
    void corsRechazaUnOrigenNoPermitido() throws Exception {
        mockMvc.perform(options("/categories/active")
                .header("Origin", "https://sitio-desconocido.com")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden());
    }
}
