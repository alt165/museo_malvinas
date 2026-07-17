ALTER TABLE objetos_museo
    DROP COLUMN IF EXISTS dimensiones,
    ADD COLUMN alto VARCHAR(80),
    ADD COLUMN ancho VARCHAR(80),
    ADD COLUMN diametro VARCHAR(80),
    ADD COLUMN espesor VARCHAR(80),
    ADD COLUMN peso VARCHAR(80),
    ADD COLUMN inscripciones VARCHAR(500),
    ADD COLUMN regimen_propiedad VARCHAR(20),
    ADD COLUMN condicion_legal_bien TEXT,
    ADD COLUMN intervenciones_inadecuadas VARCHAR(30),
    ADD COLUMN estado_integridad VARCHAR(30),
    ADD COLUMN humedad_conservacion VARCHAR(20),
    ADD COLUMN temperatura_conservacion VARCHAR(80),
    ADD COLUMN luz_conservacion VARCHAR(80),
    ADD COLUMN conservacion_extintores BOOLEAN,
    ADD COLUMN conservacion_montaje BOOLEAN,
    ADD COLUMN conservacion_sistema_electrico BOOLEAN,
    ADD COLUMN conservacion_alarmas BOOLEAN,
    ADD COLUMN conservacion_camaras BOOLEAN;

CREATE TABLE objeto_museo_detalles_conservacion (
    objeto_museo_id BIGINT NOT NULL,
    detalle VARCHAR(80) NOT NULL,
    CONSTRAINT pk_objeto_museo_detalles_conservacion PRIMARY KEY (objeto_museo_id, detalle),
    CONSTRAINT fk_objeto_museo_detalles_conservacion_objeto
        FOREIGN KEY (objeto_museo_id) REFERENCES objetos_museo(id)
);

CREATE TABLE objeto_museo_visibilidades (
    objeto_museo_id BIGINT NOT NULL,
    campo VARCHAR(80) NOT NULL,
    visibilidad VARCHAR(20) NOT NULL DEFAULT 'PUBLICO',
    CONSTRAINT pk_objeto_museo_visibilidades PRIMARY KEY (objeto_museo_id, campo),
    CONSTRAINT fk_objeto_museo_visibilidades_objeto
        FOREIGN KEY (objeto_museo_id) REFERENCES objetos_museo(id)
);
