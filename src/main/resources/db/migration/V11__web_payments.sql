-- ============================================================
-- PAGO DE PEDIDOS WEB (Yape)
-- ============================================================
-- El cliente paga al Yape de la tienda y registra el código de operación.
-- El equipo lo verifica en el ERP (UNPAID → SUBMITTED → PAID / REJECTED).
ALTER TABLE web_orders ADD COLUMN payment_status VARCHAR(12) NOT NULL DEFAULT 'UNPAID'
    CHECK (payment_status IN ('UNPAID', 'SUBMITTED', 'PAID', 'REJECTED'));
ALTER TABLE web_orders ADD COLUMN payment_operation_code VARCHAR(20);
ALTER TABLE web_orders ADD COLUMN payment_submitted_at   TIMESTAMP;
ALTER TABLE web_orders ADD COLUMN payment_verified_at    TIMESTAMP;

-- Un mismo código de operación no puede respaldar dos pedidos (salvo que el primero se haya rechazado)
CREATE UNIQUE INDEX uq_web_orders_operation_code ON web_orders (payment_operation_code)
    WHERE payment_operation_code IS NOT NULL AND payment_status IN ('SUBMITTED', 'PAID');

CREATE INDEX idx_web_orders_payment_status ON web_orders (payment_status);

-- Datos de cobro que la tienda muestra al cliente
ALTER TABLE store_settings ADD COLUMN yape_number VARCHAR(20);
ALTER TABLE store_settings ADD COLUMN yape_holder VARCHAR(100);
ALTER TABLE store_settings ADD COLUMN yape_qr_url VARCHAR(500);
