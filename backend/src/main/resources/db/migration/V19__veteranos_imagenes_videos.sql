CREATE TABLE veterano_imagen (
    id BIGSERIAL PRIMARY KEY,
    veterano_id BIGINT NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    nombre_archivo_almacenado VARCHAR(255) NOT NULL,
    tipo_contenido VARCHAR(120) NOT NULL,
    tamanio_bytes BIGINT NOT NULL,
    ruta_archivo VARCHAR(500) NOT NULL,
    ruta_relativa VARCHAR(500),
    descripcion TEXT,
    orden INTEGER NOT NULL DEFAULT 0,
    fecha_carga TIMESTAMP NOT NULL,
    cargado_por VARCHAR(160),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_eliminacion TIMESTAMP,
    CONSTRAINT fk_veterano_imagen_veterano FOREIGN KEY (veterano_id) REFERENCES veteranos(id)
);

CREATE INDEX idx_veterano_imagen_veterano ON veterano_imagen(veterano_id);
CREATE INDEX idx_veterano_imagen_activo ON veterano_imagen(activo);
CREATE INDEX idx_veterano_imagen_orden ON veterano_imagen(orden);

CREATE TABLE veterano_video (
    id BIGSERIAL PRIMARY KEY,
    veterano_id BIGINT NOT NULL,
    titulo VARCHAR(180) NOT NULL,
    url_youtube VARCHAR(500) NOT NULL,
    video_id VARCHAR(40) NOT NULL,
    descripcion TEXT,
    fecha_entrevista DATE,
    orden INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_eliminacion TIMESTAMP,
    CONSTRAINT fk_veterano_video_veterano FOREIGN KEY (veterano_id) REFERENCES veteranos(id)
);

CREATE INDEX idx_veterano_video_veterano ON veterano_video(veterano_id);
CREATE INDEX idx_veterano_video_activo ON veterano_video(activo);
CREATE INDEX idx_veterano_video_orden ON veterano_video(orden);
