-- ============================================================
-- GESTIÓN DE LA TIENDA ONLINE POR MÓDULOS
-- ============================================================
-- Catálogo web: un producto activo puede ocultarse de la tienda sin desactivarlo
-- en el ERP, y marcarse como destacado.
ALTER TABLE products ADD COLUMN web_published BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE products ADD COLUMN web_featured  BOOLEAN NOT NULL DEFAULT FALSE;

-- Ajustes de la tienda: una sola fila (id = 1) que la web lee en vivo.
CREATE TABLE store_settings (
    id               SMALLINT PRIMARY KEY CHECK (id = 1),
    whatsapp         VARCHAR(20),
    announcement     VARCHAR(200),
    payment_methods  VARCHAR(400) NOT NULL DEFAULT 'Yape / Plin,Transferencia,Efectivo al recoger',
    pickup_address   VARCHAR(255),
    business_hours   VARCHAR(150),
    shipping_note    VARCHAR(255),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO store_settings (id) VALUES (1);
