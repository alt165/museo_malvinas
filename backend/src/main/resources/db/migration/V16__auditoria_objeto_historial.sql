ALTER TABLE auditorias
    ADD COLUMN accion VARCHAR(80),
    ADD COLUMN descripcion TEXT,
    ADD COLUMN origen VARCHAR(80),
    ADD COLUMN numero_inventario VARCHAR(80),
    ADD COLUMN usuario_identificador VARCHAR(160),
    ADD COLUMN usuario_nombre VARCHAR(200),
    ADD COLUMN rol VARCHAR(80);

CREATE INDEX idx_auditoria_objeto_fecha ON auditorias (entidad, entidad_id, fecha DESC);
