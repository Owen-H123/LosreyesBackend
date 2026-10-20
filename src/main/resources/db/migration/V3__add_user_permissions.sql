-- ============================================================
-- PERMISOS INDIVIDUALES POR TRABAJADOR
-- ============================================================
-- Cada fila = un permiso EXTRA otorgado a un usuario WORKER, por encima
-- de lo que le da su rol base (que por defecto no tiene ninguno de estos).
-- Ausencia de fila = no lo tiene. ADMIN no usa esta tabla: siempre tiene
-- acceso completo por su rol, sin necesidad de filas aquí.
CREATE TABLE user_permissions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_code VARCHAR(50) NOT NULL
                        CHECK (permission_code IN (
                            'VIEW_AUDIT', 'VIEW_REPORTS', 'ADJUST_INVENTORY',
                            'DELETE_PRODUCTS', 'CANCEL_SALES', 'MANAGE_CATEGORIES',
                            'EDIT_SALES'
                        )),
    granted_by      BIGINT REFERENCES users(id),
    granted_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, permission_code)
);

CREATE INDEX idx_user_permissions_user_id ON user_permissions(user_id);