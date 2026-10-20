-- ============================================================
-- PEDIDOS DE LA TIENDA ONLINE
-- ============================================================
-- El cliente arma el carrito en la web y envía el pedido. Queda aquí con
-- estado NEW hasta que la tienda lo confirma por WhatsApp. No descuenta stock:
-- eso ocurre cuando se registra la venta en el ERP.
--
-- Los precios se copian del producto al momento del pedido (el servidor los
-- calcula; no se confía en el precio que manda el navegador).
CREATE TABLE web_orders (
    id             BIGSERIAL PRIMARY KEY,
    order_number   VARCHAR(20)   NOT NULL UNIQUE,
    customer_name  VARCHAR(150)  NOT NULL,
    phone          VARCHAR(20)   NOT NULL,
    delivery       VARCHAR(10)   NOT NULL CHECK (delivery IN ('SHIPPING', 'PICKUP')),
    address        VARCHAR(255),
    district       VARCHAR(100),
    car            VARCHAR(120),
    payment_method VARCHAR(40)   NOT NULL,
    total          NUMERIC(12,2) NOT NULL,
    status         VARCHAR(15)   NOT NULL DEFAULT 'NEW'
                   CHECK (status IN ('NEW', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE web_order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT        NOT NULL REFERENCES web_orders(id) ON DELETE CASCADE,
    product_id   BIGINT        NOT NULL REFERENCES products(id),
    product_code VARCHAR(50)   NOT NULL,
    product_name VARCHAR(200)  NOT NULL,
    unit_price   NUMERIC(12,2) NOT NULL,
    quantity     INT           NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_web_orders_status  ON web_orders(status);
CREATE INDEX idx_web_orders_created ON web_orders(created_at);
CREATE INDEX idx_web_order_items_order ON web_order_items(order_id);

CREATE SEQUENCE web_order_number_seq START 1;
