package com.delosreyes.inventario.modules.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ciclo de la sesión con el refresh token en una cookie HttpOnly:
 * login → la cookie llega → refresh con la cookie → logout la borra.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RefreshTokenCookieFlowTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void elLoginEntregaElRefreshTokenEnUnaCookieHttpOnlyYNoEnElJson() throws Exception {
        MvcResult login = login();

        String setCookie = login.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
            .startsWith(RefreshTokenCookie.NAME + "=")
            .contains("HttpOnly")
            .contains("SameSite=Lax")
            .contains("Path=/api/v1/auth");

        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        assertThat(body.hasNonNull("accessToken")).isTrue();
        assertThat(body.has("refreshToken")).as("el JSON no debe incluir el refresh token").isFalse();
    }

    @Test
    void conLaCookieSeRenuevaElAccessToken() throws Exception {
        Cookie cookie = refreshCookieFrom(login());

        MvcResult refresh = mockMvc.perform(post("/auth/refresh").cookie(cookie))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode body = objectMapper.readTree(refresh.getResponse().getContentAsString());
        assertThat(body.hasNonNull("accessToken")).isTrue();
        assertThat(body.has("refreshToken")).isFalse();
    }

    @Test
    void sinCookieLaRenovacionResponde401() throws Exception {
        mockMvc.perform(post("/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void conUnaCookieInventadaLaRenovacionResponde401() throws Exception {
        mockMvc.perform(post("/auth/refresh").cookie(new Cookie(RefreshTokenCookie.NAME, "inventado")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void elLogoutBorraLaCookieYElTokenDejaDeServir() throws Exception {
        MvcResult login = login();
        Cookie cookie = refreshCookieFrom(login);
        String accessToken = objectMapper.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();

        MvcResult logout = mockMvc.perform(post("/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isNoContent())
            .andReturn();

        assertThat(logout.getResponse().getHeader(HttpHeaders.SET_COOKIE))
            .startsWith(RefreshTokenCookie.NAME + "=;")
            .contains("Max-Age=0");

        // Aunque alguien hubiera guardado la cookie, ya está revocada en la base
        mockMvc.perform(post("/auth/refresh").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    private MvcResult login() throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@delosreyes.com\",\"password\":\"Admin2024!\"}"))
            .andExpect(status().isOk())
            .andReturn();
    }

    private Cookie refreshCookieFrom(MvcResult login) {
        String setCookie = login.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String value = setCookie.substring(RefreshTokenCookie.NAME.length() + 1, setCookie.indexOf(';'));
        return new Cookie(RefreshTokenCookie.NAME, value);
    }
}
