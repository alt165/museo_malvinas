CREATE TABLE colecciones_objetos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(160) NOT NULL,
    descripcion TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_eliminacion TIMESTAMP
);

ALTER TABLE objetos_museo
    ADD COLUMN coleccion_id BIGINT;

ALTER TABLE objetos_museo
    ADD CONSTRAINT fk_objetos_museo_coleccion
    FOREIGN KEY (coleccion_id) REFERENCES colecciones_objetos (id);

CREATE INDEX idx_objetos_museo_coleccion ON objetos_museo (coleccion_id);
CREATE UNIQUE INDEX uk_colecciones_objetos_nombre_activo
    ON colecciones_objetos (LOWER(nombre))
    WHERE eliminado = FALSE;
