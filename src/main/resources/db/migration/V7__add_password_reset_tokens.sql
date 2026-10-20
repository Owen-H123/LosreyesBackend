-- ============================================================
-- RECUPERACIÓN DE CONTRASEÑA
-- ============================================================
-- Cada solicitud de "olvidé mi contraseña" genera un token aleatorio que se
-- envía por correo. Aquí se guarda solo su hash SHA-256: si alguien lee esta
-- tabla, no puede usar los tokens.
--
-- Un token sirve una sola vez (used_at) y vence (expires_at).
CREATE TABLE password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    used_at     TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens(user_id);
