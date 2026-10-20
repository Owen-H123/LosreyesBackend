-- ============================================================
-- V2__add_constraints.sql
-- Restricciones adicionales y ajustes de esquema
-- ============================================================

-- Restricción: precio de venta debe ser >= precio de costo
ALTER TABLE products
    ADD CONSTRAINT chk_sale_price_gte_cost
    CHECK (sale_price >= cost_price);

-- Restricción: stock no puede ser negativo
ALTER TABLE products
    ADD CONSTRAINT chk_stock_non_negative
    CHECK (stock >= 0);

-- Restricción: mínimo stock no puede ser negativo
ALTER TABLE products
    ADD CONSTRAINT chk_min_stock_non_negative
    CHECK (minimum_stock >= 0);

-- Restricción: total de venta no puede ser negativo
ALTER TABLE sales
    ADD CONSTRAINT chk_sale_total_non_negative
    CHECK (total >= 0);

-- Restricción: cantidad en items de venta debe ser > 0
ALTER TABLE sale_items
    ADD CONSTRAINT chk_sale_item_quantity_positive
    CHECK (quantity > 0);

-- Restricción: cantidad en movimientos de inventario
ALTER TABLE inventory_movements
    ADD CONSTRAINT chk_movement_quantity_positive
    CHECK (quantity > 0);

-- Índice para búsqueda de refresh tokens no expirados
CREATE INDEX idx_refresh_valid
    ON refresh_tokens (user_id, revoked, expires_at)
    WHERE revoked = FALSE;

-- Índice para dashboard: ventas completadas por fecha
CREATE INDEX idx_sales_completed_date
    ON sales (sale_date, status)
    WHERE status = 'COMPLETED';

-- Índice para productos activos con stock
CREATE INDEX idx_products_active_stock
    ON products (stock, minimum_stock, status)
    WHERE deleted = FALSE AND status = 'ACTIVE';