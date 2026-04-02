-- V9: Ampliar tipos permitidos en notificaciones.tipo

ALTER TABLE notificaciones
DROP CONSTRAINT IF EXISTS chk_tipo;

ALTER TABLE notificaciones
DROP CONSTRAINT IF EXISTS notificaciones_tipo_check;

ALTER TABLE notificaciones
ADD CONSTRAINT chk_tipo
CHECK (tipo IN (
    'NUEVA_ACTIVIDAD',
    'NUEVO_ENTREGABLE',
    'DEADLINE_CERCANO',
    'ENTREGA_EVALUADA',
    'NOTA_PUBLICADA'
));
