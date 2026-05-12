ALTER TABLE objetos_museo
    ADD COLUMN eliminado_por VARCHAR(120);

CREATE INDEX idx_objetos_museo_eliminado ON objetos_museo (eliminado);
CREATE INDEX idx_objetos_museo_fecha_eliminacion ON objetos_museo (fecha_eliminacion);
