-- V1: Añadir columnas de almacenamiento en la nube a la tabla materiales
-- Columnas necesarias para la integración con OneDrive y Cloudinary
-- Usamos IF NOT EXISTS para garantizar idempotencia (seguro re-ejecutar)

ALTER TABLE materiales ADD COLUMN IF NOT EXISTS onedrive_owner_id     BIGINT;
ALTER TABLE materiales ADD COLUMN IF NOT EXISTS cloudinary_public_id  VARCHAR(255);
ALTER TABLE materiales ADD COLUMN IF NOT EXISTS cloudinary_url        VARCHAR(255);
