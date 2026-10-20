-- ============================================================
-- CORRELATIVO DE NÚMERO DE VENTA
-- ============================================================
-- El correlativo de VTA-YYYYMMDD-XXXXX vive en la base de datos para que no
-- vuelva a empezar en 1 cuando se reinicia el backend.
--
-- Arranca en MAX(id) + 1: cada venta existente usó un correlativo menor o
-- igual a la cantidad de ventas creadas, así que no choca con ninguna.
CREATE SEQUENCE sale_number_seq START WITH 1;

SELECT setval('sale_number_seq', COALESCE((SELECT MAX(id) FROM sales), 0) + 1, false);
