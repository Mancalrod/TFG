-- Agregar campo para comentario/observaciones del alumno en entregas
-- Permite entregas de solo texto o agregar notas a entregas con archivos
ALTER TABLE entregas ADD COLUMN comentario_alumno TEXT;
