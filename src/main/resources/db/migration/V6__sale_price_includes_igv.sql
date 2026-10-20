-- ============================================================
-- PRECIOS DE VENTA CON IGV INCLUIDO
-- ============================================================
-- Desde esta versión, products.sale_price guarda el precio CON IGV (el que
-- paga el cliente) y el IGV de la venta se desglosa del total en lugar de
-- sumarse al final (ver IgvCalculator).
--
-- Los precios existentes estaban sin IGV: se convierten para que los
-- totales que paga el cliente sigan siendo los mismos.
-- Ejemplo: S/ 80.00 sin IGV -> S/ 94.40 con IGV.
--
-- Las ventas ya registradas (sales, sale_items) no se modifican.
UPDATE products
SET sale_price = ROUND(sale_price * 1.18, 2),
    updated_at = NOW();
