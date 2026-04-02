-- V10: Campos de destino para navegación contextual en notificaciones

ALTER TABLE notificaciones
ADD COLUMN IF NOT EXISTS actividad_id BIGINT,
ADD COLUMN IF NOT EXISTS entregable_id BIGINT,
ADD COLUMN IF NOT EXISTS entrega_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_notificaciones_actividad ON notificaciones(actividad_id);
CREATE INDEX IF NOT EXISTS idx_notificaciones_entregable ON notificaciones(entregable_id);
CREATE INDEX IF NOT EXISTS idx_notificaciones_entrega ON notificaciones(entrega_id);
