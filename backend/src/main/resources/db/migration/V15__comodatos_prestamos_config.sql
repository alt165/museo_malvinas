CREATE TABLE configuracion_sistema (
    clave VARCHAR(120) PRIMARY KEY,
    valor VARCHAR(255) NOT NULL,
    descripcion VARCHAR(255)
);

INSERT INTO configuracion_sistema (clave, valor, descripcion)
VALUES ('comodatos_prestamos.dias_alerta', '14', 'Dias de anticipacion para alertas de comodatos y prestamos proximos a vencer');
