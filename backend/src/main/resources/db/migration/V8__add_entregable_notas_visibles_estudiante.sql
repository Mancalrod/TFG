ALTER TABLE entregables
ADD COLUMN IF NOT EXISTS notas_visibles_estudiante BOOLEAN DEFAULT FALSE;
