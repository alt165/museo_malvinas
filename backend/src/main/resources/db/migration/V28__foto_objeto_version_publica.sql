ALTER TABLE fotos_objeto_museo
    ADD COLUMN ruta_publica VARCHAR(500),
    ADD COLUMN content_type_publico VARCHAR(120),
    ADD COLUMN tamanio_bytes_publico BIGINT;
