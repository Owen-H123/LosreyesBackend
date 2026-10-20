package com.delosreyes.inventario.security.service;

import com.delosreyes.inventario.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de la generación y validación de tokens JWT.
 * Es la base de la seguridad: un token alterado o vencido no debe servir.
 */
class JwtServiceTest {

    private static final String SECRET = "clave-de-prueba-de-al-menos-32-caracteres-123";

    private JwtService jwtService;
    private final UserDetails cajero = user("cajero@delosreyes.com");

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(properties(SECRET, 60_000));
    }

    @Test
    void unTokenRecienGeneradoEsValidoParaSuUsuario() {
        String token = jwtService.generateAccessToken(cajero);

        assertThat(jwtService.extractUsername(token)).isEqualTo("cajero@delosreyes.com");
        assertThat(jwtService.isTokenValid(token, cajero)).isTrue();
    }

    @Test
    void elTokenDeUnUsuarioNoSirveParaOtro() {
        String token = jwtService.generateAccessToken(cajero);

        assertThat(jwtService.isTokenValid(token, user("admin@delosreyes.com"))).isFalse();
    }

    @Test
    void unTokenVencidoNoEsValido() {
        JwtService vencido = new JwtService(properties(SECRET, -1_000));   // ya expiró al crearse
        String token = vencido.generateAccessToken(cajero);

        assertThat(vencido.isTokenValid(token, cajero)).isFalse();
    }

    @Test
    void unTokenAlteradoSeRechaza() {
        String token = jwtService.generateAccessToken(cajero);
        String[] parts = token.split("\\.");
        // Se cambia la firma: cualquier modificación debe invalidar el token
        String alterado = parts[0] + "." + parts[1] + "." + parts[2].substring(0, parts[2].length() - 2) + "xx";

        assertThat(jwtService.isTokenValid(alterado, cajero)).isFalse();
        assertThatThrownBy(() -> jwtService.extractUsername(alterado)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void unTokenFirmadoConOtraClaveSeRechaza() {
        JwtService otroServidor = new JwtService(properties("otra-clave-distinta-de-al-menos-32-caracteres", 60_000));
        String token = otroServidor.generateAccessToken(cajero);

        assertThat(jwtService.isTokenValid(token, cajero)).isFalse();
    }

    @Test
    void dosTokensDelMismoUsuarioEnElMismoSegundoSonDistintos() {
        // Antes salían idénticos y el segundo login fallaba por el UNIQUE de refresh_tokens
        assertThat(jwtService.generateRefreshToken(cajero)).isNotEqualTo(jwtService.generateRefreshToken(cajero));
        assertThat(jwtService.generateAccessToken(cajero)).isNotEqualTo(jwtService.generateAccessToken(cajero));
    }

    @Test
    void unaClaveCortaImpideArrancar() {
        assertThatThrownBy(() -> new JwtService(properties("corta", 60_000)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("demasiado corto");
    }

    private static JwtProperties properties(String secret, long expirationMs) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        properties.setExpiration(expirationMs);
        properties.setRefreshExpiration(expirationMs);
        properties.setIssuer("delosreyes-erp");
        return properties;
    }

    private static UserDetails user(String email) {
        return new User(email, "x", List.of());
    }
}
