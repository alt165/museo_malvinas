ALTER TABLE relaciones_objetos
    ADD COLUMN fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN creado_por VARCHAR(160);

ALTER TABLE relaciones_objetos
    DROP CONSTRAINT IF EXISTS uk_relacion_objeto_direccional;

CREATE UNIQUE INDEX uk_relacion_objeto_direccional_activa
    ON relaciones_objetos (objeto_origen_id, objeto_destino_id, tipo_relacion)
    WHERE eliminado = FALSE;

CREATE INDEX idx_relacion_objeto_tipo ON relaciones_objetos (tipo_relacion);
