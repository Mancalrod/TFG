ALTER TABLE actividades ADD COLUMN modo_onedrive VARCHAR(20) DEFAULT 'ACTIVIDAD';
ALTER TABLE entregables ADD COLUMN carpeta_onedrive VARCHAR(255);
