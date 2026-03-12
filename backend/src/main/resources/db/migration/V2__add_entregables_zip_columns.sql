-- V2: Añadir columnas de estructura ZIP a la tabla entregables
-- Columnas necesarias para la validación de estructura de archivos ZIP entregados
-- Usamos IF NOT EXISTS para garantizar idempotencia (seguro re-ejecutar)

ALTER TABLE entregables ADD COLUMN IF NOT EXISTS estructura_zip          TEXT;
ALTER TABLE entregables ADD COLUMN IF NOT EXISTS nombre_zip_esperado     VARCHAR(255);
ALTER TABLE entregables ADD COLUMN IF NOT EXISTS validacion_zip_estricta BOOLEAN DEFAULT FALSE;
