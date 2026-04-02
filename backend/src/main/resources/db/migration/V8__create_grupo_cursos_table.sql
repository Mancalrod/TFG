-- Permite asociar un grupo a varios cursos.
CREATE TABLE IF NOT EXISTS grupo_cursos (
    grupo_id BIGINT NOT NULL,
    curso_id BIGINT NOT NULL,
    CONSTRAINT pk_grupo_cursos PRIMARY KEY (grupo_id, curso_id),
    CONSTRAINT fk_grupo_cursos_grupo FOREIGN KEY (grupo_id) REFERENCES grupos(id) ON DELETE CASCADE,
    CONSTRAINT fk_grupo_cursos_curso FOREIGN KEY (curso_id) REFERENCES cursos(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_grupo_cursos_curso_id ON grupo_cursos(curso_id);

-- Backfill inicial: cada grupo mantiene su curso principal como curso asociado.
INSERT INTO grupo_cursos (grupo_id, curso_id)
SELECT g.id, g.curso_id
FROM grupos g
WHERE g.curso_id IS NOT NULL
ON CONFLICT DO NOTHING;
