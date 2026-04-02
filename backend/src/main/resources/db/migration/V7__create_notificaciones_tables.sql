-- V7: Crear tablas de notificaciones y preferencias

CREATE TABLE preferencias_notificacion (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE,
    canal VARCHAR(10) NOT NULL DEFAULT 'APP',
    CONSTRAINT fk_pref_notif_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT chk_canal CHECK (canal IN ('APP', 'EMAIL', 'AMBOS'))
);

CREATE TABLE notificaciones (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    titulo VARCHAR(300) NOT NULL,
    mensaje TEXT,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    curso_id BIGINT,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_curso FOREIGN KEY (curso_id) REFERENCES cursos(id) ON DELETE SET NULL,
    CONSTRAINT chk_tipo CHECK (tipo IN ('NUEVO_ENTREGABLE', 'DEADLINE_CERCANO'))
);

CREATE INDEX idx_notificaciones_usuario ON notificaciones(usuario_id);
CREATE INDEX idx_notificaciones_leida ON notificaciones(usuario_id, leida);
