CREATE TABLE embargos_objeto (
    id BIGSERIAL PRIMARY KEY,
    objeto_museo_id BIGINT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_finalizacion DATE,
    observaciones TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_eliminacion TIMESTAMP,
    CONSTRAINT fk_embargos_objeto_objeto
        FOREIGN KEY (objeto_museo_id) REFERENCES objetos_museo(id)
);

CREATE UNIQUE INDEX uk_embargos_objeto_vigente
    ON embargos_objeto (objeto_museo_id)
    WHERE fecha_finalizacion IS NULL AND eliminado = FALSE;

CREATE INDEX idx_embargos_objeto_objeto ON embargos_objeto (objeto_museo_id);
CREATE INDEX idx_embargos_objeto_vigente ON embargos_objeto (fecha_finalizacion);
