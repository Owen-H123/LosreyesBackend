package com.delosreyes.inventario.security.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.delosreyes.inventario.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int MIN_SECRET_LENGTH_BYTES = 32;

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = buildSigningKey();
    }

    // VALIDACIÓN SECRET 

    private SecretKey buildSigningKey() {
        String secret = jwtProperties.getSecret();

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET no está configurado");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < MIN_SECRET_LENGTH_BYTES) {
            throw new IllegalStateException(
                "JWT_SECRET demasiado corto. Usa mínimo 32 caracteres (HS256)."
            );
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    // TOKENS

    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(userDetails, new HashMap<>());
    }

    //  ESTE ES EL QUE TE FALTABA (FIX DEL ERROR DE COMPILACIÓN)
    public String generateAccessToken(UserDetails userDetails, Map<String, Object> extraClaims) {

        if (extraClaims == null) {
            extraClaims = new HashMap<>();
        }

        extraClaims.put("type", "access");

        return buildToken(userDetails, jwtProperties.getExpiration(), extraClaims);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        return buildToken(userDetails, jwtProperties.getRefreshExpiration(), claims);
    }

    private String buildToken(
            UserDetails userDetails,
            long expiration,
            Map<String, Object> claims
    ) {
        return Jwts.builder()
            // jti único: sin él, dos tokens del mismo usuario emitidos en el mismo
            // segundo salían idénticos (las fechas van en segundos) y el segundo
            // refresh token chocaba con el UNIQUE de refresh_tokens.token
            .id(UUID.randomUUID().toString())
            .subject(userDetails.getUsername())
            .claims(claims)
            .issuer(jwtProperties.getIssuer())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(signingKey)
            .compact();
    }

    // ================= CLAIMS =================

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    private Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException e) {
            throw new RuntimeException("JWT inválido: " + e.getMessage());
        }
    }

    // ================= VALIDACIÓN =================

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);

            return username.equals(userDetails.getUsername())
                && !extractExpiration(token).before(new Date());

        } catch (Exception e) {
            return false;
        }
    }
}