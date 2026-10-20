-- ============================================================
-- De los Reyes ERP — Migración inicial de base de datos
-- V1__init_schema.sql
--
-- Crea el esquema completo del sistema:
-- usuarios, roles, productos, categorías, vehículos,
-- inventario, movimientos, ventas, clientes, auditoría
-- ============================================================
-- ============================================================
-- ROLES
-- ============================================================
CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- USUARIOS
-- ============================================================
CREATE TABLE users (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(150) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    role_id      BIGINT NOT NULL REFERENCES roles(id),
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                     CHECK (status IN ('ACTIVE', 'SUSPENDED', 'INACTIVE')),
    last_login   TIMESTAMP,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- REFRESH TOKENS
-- ============================================================
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(500) NOT NULL UNIQUE,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    expires_at  TIMESTAMP NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- CATEGORÍAS
-- ============================================================
CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- VEHÍCULOS (compatibilidad de productos)
-- ============================================================
CREATE TABLE vehicles (
    id      BIGSERIAL PRIMARY KEY,
    brand   VARCHAR(50) NOT NULL,
    model   VARCHAR(100) NOT NULL,
    manufacture_year INTEGER NOT NULL,
    engine  VARCHAR(50),
    UNIQUE (brand, model, manufacture_year, engine)
);

-- ============================================================
-- PRODUCTOS
-- ============================================================
CREATE TABLE products (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(50) NOT NULL UNIQUE,
    name          VARCHAR(200) NOT NULL,
    description   TEXT,
    brand         VARCHAR(100),
    category_id   BIGINT NOT NULL REFERENCES categories(id),
    cost_price    NUMERIC(12,2) NOT NULL DEFAULT 0,
    sale_price    NUMERIC(12,2) NOT NULL DEFAULT 0,
    stock         INTEGER NOT NULL DEFAULT 0,
    minimum_stock INTEGER NOT NULL DEFAULT 5,
    image_url     VARCHAR(500),
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                      CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISCONTINUED')),
    deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- PRODUCTO-VEHÍCULO (many-to-many)
-- ============================================================
CREATE TABLE product_vehicles (
    product_id BIGINT NOT NULL REFERENCES products(id),
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id),
    PRIMARY KEY (product_id, vehicle_id)
);

-- ============================================================
-- CLIENTES
-- ============================================================
CREATE TABLE customers (
    id           BIGSERIAL PRIMARY KEY,
    document_type VARCHAR(10) NOT NULL DEFAULT 'DNI'
                      CHECK (document_type IN ('DNI','RUC','CE','PASSPORT')),
    document_number VARCHAR(20) NOT NULL UNIQUE,
    name         VARCHAR(200) NOT NULL,
    email        VARCHAR(150),
    phone        VARCHAR(20),
    address      TEXT,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- VENTAS
-- ============================================================
CREATE TABLE sales (
    id            BIGSERIAL PRIMARY KEY,
    sale_number   VARCHAR(20) NOT NULL UNIQUE,
    customer_id   BIGINT REFERENCES customers(id),
    user_id       BIGINT NOT NULL REFERENCES users(id),
    status        VARCHAR(20) NOT NULL DEFAULT 'COMPLETED'
                      CHECK (status IN ('PENDING','COMPLETED','CANCELLED','REFUNDED')),
    subtotal      NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount      NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax           NUMERIC(12,2) NOT NULL DEFAULT 0,
    total         NUMERIC(12,2) NOT NULL DEFAULT 0,
    payment_method VARCHAR(30) NOT NULL DEFAULT 'CASH'
                      CHECK (payment_method IN ('CASH','CARD','TRANSFER','CREDIT')),
    notes         TEXT,
    sale_date     TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- DETALLE DE VENTAS
-- ============================================================
CREATE TABLE sale_items (
    id          BIGSERIAL PRIMARY KEY,
    sale_id     BIGINT NOT NULL REFERENCES sales(id),
    product_id  BIGINT NOT NULL REFERENCES products(id),
    quantity    INTEGER NOT NULL,
    unit_price  NUMERIC(12,2) NOT NULL,
    discount    NUMERIC(12,2) NOT NULL DEFAULT 0,
    subtotal    NUMERIC(12,2) NOT NULL
);

-- ============================================================
-- MOVIMIENTOS DE INVENTARIO
-- ============================================================
CREATE TABLE inventory_movements (
    id           BIGSERIAL PRIMARY KEY,
    product_id   BIGINT NOT NULL REFERENCES products(id),
    user_id      BIGINT NOT NULL REFERENCES users(id),
    sale_id      BIGINT REFERENCES sales(id),
    type         VARCHAR(20) NOT NULL
                     CHECK (type IN ('ENTRY','EXIT','SALE','ADJUSTMENT')),
    quantity     INTEGER NOT NULL,
    stock_before INTEGER NOT NULL,
    stock_after  INTEGER NOT NULL,
    unit_cost    NUMERIC(12,2),
    observation  TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- AUDITORÍA
-- ============================================================
CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users(id),
    user_email  VARCHAR(150),
    module      VARCHAR(50) NOT NULL,
    action      VARCHAR(50) NOT NULL,
    entity_id   VARCHAR(50),
    description TEXT,
    metadata    TEXT,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- ÍNDICES para rendimiento
-- ============================================================
CREATE INDEX idx_products_code ON products(code);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_status ON products(status) WHERE deleted = FALSE;
CREATE INDEX idx_products_stock ON products(stock);
CREATE INDEX idx_products_deleted ON products(deleted);

CREATE INDEX idx_sales_date ON sales(sale_date);
CREATE INDEX idx_sales_user ON sales(user_id);
CREATE INDEX idx_sales_customer ON sales(customer_id);
CREATE INDEX idx_sales_status ON sales(status);
CREATE INDEX idx_sales_number ON sales(sale_number);

CREATE INDEX idx_sale_items_sale ON sale_items(sale_id);
CREATE INDEX idx_sale_items_product ON sale_items(product_id);

CREATE INDEX idx_movements_product ON inventory_movements(product_id);
CREATE INDEX idx_movements_date ON inventory_movements(created_at);
CREATE INDEX idx_movements_type ON inventory_movements(type);
CREATE INDEX idx_movements_user ON inventory_movements(user_id);

CREATE INDEX idx_audit_module ON audit_logs(module);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

CREATE INDEX idx_refresh_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);

CREATE INDEX idx_vehicles_brand ON vehicles(brand);
CREATE INDEX idx_vehicles_model ON vehicles(model);

-- ============================================================
-- DATOS INICIALES
-- ============================================================

-- Roles
INSERT INTO roles (name, description) VALUES
('ADMIN', 'Administrador del sistema con acceso total'),
('WORKER', 'Trabajador con acceso limitado a operaciones diarias');

-- Usuario administrador por defecto
-- Password: Admin2024! (bcrypt $2a$12$...)
INSERT INTO users (name, email, password, role_id, status) VALUES
('Administrador',
 'admin@delosreyes.com',
 '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewFkbyZ4p1DsKVi6',
 1,
 'ACTIVE');

-- Categorías base
INSERT INTO categories (name, description) VALUES
('Frenos', 'Pastillas, discos, zapatas, bombas de freno'),
('Motor', 'Filtros, juntas, correas, bujías, bobinas'),
('Suspensión', 'Amortiguadores, rotulas, bujes, estabilizadores'),
('Transmisión', 'Embragues, cajas de cambios, diferencial'),
('Eléctrico', 'Alternadores, arranques, sensores, cables'),
('Carrocería', 'Paragolpes, espejos, molduras, clips'),
('Lubricantes', 'Aceites de motor, transmisión, dirección'),
('Refrigeración', 'Radiadores, termostatos, mangueras, bomba de agua'),
('Dirección', 'Terminales, barra de dirección, caja de dirección'),
('Accesorios', 'Fundas, tapetes, accesorios varios');

-- Vehículos compatibles base
INSERT INTO vehicles (brand, model, manufacture_year, engine) VALUES
('Peugeot', '206', 2000, '1.4'),
('Peugeot', '206', 2000, '1.6'),
('Peugeot', '207', 2007, '1.4'),
('Peugeot', '207', 2007, '1.6'),
('Peugeot', '307', 2001, '1.6'),
('Peugeot', '307', 2001, '2.0'),
('Peugeot', '308', 2008, '1.6'),
('Peugeot', '308', 2008, '2.0'),
('Peugeot', 'Partner', 2003, '1.4'),
('Peugeot', 'Partner', 2003, '1.6'),
('Citroën', 'C3', 2002, '1.4'),
('Citroën', 'C3', 2002, '1.6'),
('Citroën', 'C4', 2004, '1.6'),
('Citroën', 'C4', 2004, '2.0'),
('Citroën', 'Berlingo', 2002, '1.4'),
('Citroën', 'Berlingo', 2002, '1.6'),
('Citroën', 'Picasso', 2000, '1.6'),
('Citroën', 'Picasso', 2000, '2.0'),
('Renault', 'Clio', 1999, '1.2'),
('Renault', 'Clio', 1999, '1.6'),
('Renault', 'Logan', 2005, '1.4'),
('Renault', 'Logan', 2005, '1.6'),
('Renault', 'Sandero', 2008, '1.6'),
('Renault', 'Duster', 2011, '1.6'),
('Renault', 'Duster', 2011, '2.0'),
('Renault', 'Megane', 2003, '1.6'),
('Renault', 'Megane', 2003, '2.0');