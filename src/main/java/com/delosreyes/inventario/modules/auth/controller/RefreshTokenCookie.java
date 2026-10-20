package com.delosreyes.inventario.modules.auth.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.delosreyes.inventario.config.JwtProperties;

/**
 * RefreshTokenCookie — Arma la cookie que guarda el refresh token.
 *
 * POR QUÉ UNA COOKIE:
 *   Antes el refresh token (7 días) se guardaba en localStorage, donde
 *   cualquier script de la página puede leerlo. En una cookie HttpOnly el
 *   navegador la envía solo y JavaScript no la puede leer.
 *
 * ATRIBUTOS:
 *   HttpOnly     → JavaScript no la puede leer.
 *   SameSite=Lax → no se envía en peticiones que vienen de otros sitios.
 *   Path         → solo se envía a /api/v1/auth (refresh y logout), no a
 *                  toda la API.
 *   Secure       → solo por HTTPS. Configurable (REFRESH_COOKIE_SECURE)
 *                  porque en la red local el sistema corre por HTTP.
 */
@Component
public class RefreshTokenCookie {

    public static final String NAME = "refresh_token";

    private final String path;
    private final boolean secure;
    private final Duration maxAge;

    public RefreshTokenCookie(
            @Value("${server.servlet.context-path:}") String contextPath,
            @Value("${app.auth.refresh-cookie.secure:false}") boolean secure,
            JwtProperties jwtProperties) {
        this.path = contextPath + "/auth";
        this.secure = secure;
        this.maxAge = Duration.ofMillis(jwtProperties.getRefreshExpiration());
    }

    /** Cookie con el refresh token, para la respuesta del login. */
    public ResponseCookie create(String refreshToken) {
        return base(refreshToken).maxAge(maxAge).build();
    }

    /** Cookie vacía que vence ya: el navegador la borra (logout). */
    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path(path);
    }
}
