ALTER TABLE objetos_museo
    ADD COLUMN origen_carga VARCHAR(20) NOT NULL DEFAULT 'COMPLETA',
    ADD COLUMN datos_completos BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN fecha_carga_rapida TIMESTAMP,
    ADD COLUMN carga_rapida_por VARCHAR(160);

CREATE INDEX idx_objetos_museo_pendientes_completar
    ON objetos_museo (origen_carga, datos_completos, eliminado);

CREATE INDEX idx_objetos_museo_fecha_carga_rapida
    ON objetos_museo (fecha_carga_rapida);
