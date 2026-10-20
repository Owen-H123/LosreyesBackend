    package com.delosreyes.inventario.config;

    import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

    /**
     * JwtProperties — Enlaza las propiedades jwt.* del application.yml a un bean.
     *
     * QUÉ HACE:
     *   Permite inyectar propiedades JWT tipadas en cualquier componente del sistema.
     *
     * POR QUÉ EXISTE:
     *   Centraliza la configuración JWT. Si cambia el secreto o expiración,
     *   solo se modifica application.yml sin tocar lógica.
     *
     * RELACIONES:
     *   Usado por JwtService para firmar y validar tokens.
     */
@Component
    @ConfigurationProperties(prefix = "jwt")
    public class JwtProperties {

        private String secret;
        private long expiration;
        private long refreshExpiration;
        private String issuer;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }

        public long getExpiration() { return expiration; }
        public void setExpiration(long expiration) { this.expiration = expiration; }

        public long getRefreshExpiration() { return refreshExpiration; }
        public void setRefreshExpiration(long refreshExpiration) { this.refreshExpiration = refreshExpiration; }

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
    }