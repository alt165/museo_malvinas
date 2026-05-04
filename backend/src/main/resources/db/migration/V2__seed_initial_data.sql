INSERT INTO categoria_objeto (id, nombre, descripcion)
VALUES
    (1, 'Documentos', 'Cartas, certificados, mapas y documentos vinculados al conflicto.'),
    (2, 'Fotografias', 'Registros fotograficos historicos y material visual.'),
    (3, 'Indumentaria', 'Uniformes, cascos, abrigos y accesorios personales.'),
    (4, 'Equipamiento militar', 'Elementos de campania, comunicaciones y equipamiento operativo.'),
    (5, 'Objetos personales', 'Pertenencias, recuerdos y elementos de uso personal.'),
    (6, 'Reconocimientos', 'Medallas, diplomas y distinciones.');

INSERT INTO ubicaciones (id, nombre, tipo, descripcion)
VALUES
    (1, 'Deposito principal', 'DEPOSITO', 'Ubicacion base para resguardo general de piezas.'),
    (2, 'Sala Malvinas', 'SALA', 'Sala principal de exhibicion permanente.'),
    (3, 'Reserva tecnica', 'RESERVA', 'Area de guarda para piezas sensibles o en preparacion.'),
    (4, 'Taller de conservacion', 'TALLER', 'Area de revision, limpieza y restauracion.'),
    (5, 'Archivo documental', 'ARCHIVO', 'Guarda de documentos, cartas, mapas y fotografias.');

INSERT INTO depositantes (id, nombre, tipo, contacto, observaciones)
VALUES
    (1, 'Juan Carlos Perez', 'PERSONA', 'juan.perez@example.com', 'Depositante de prueba para donaciones personales.'),
    (2, 'Centro de Veteranos de Malvinas Salta', 'INSTITUCION', 'contacto@veteranos-salta.example.com', 'Institucion de prueba para cesiones y donaciones.'),
    (3, 'Maria Elena Gomez', 'PERSONA', 'maria.gomez@example.com', 'Familiar de veterano, registro de prueba.');

INSERT INTO veteranos (id, nombre, apellido, fuerza, fecha_nacimiento, fecha_fallecimiento, historia)
VALUES
    (1, 'Roberto', 'Sanchez', 'EJERCITO', DATE '1959-08-12', NULL, 'Veterano de prueba asociado a piezas de campania.'),
    (2, 'Miguel Angel', 'Ferreyra', 'ARMADA', DATE '1961-03-25', NULL, 'Veterano de prueba asociado a documentacion naval.'),
    (3, 'Carlos Alberto', 'Rios', 'FUERZA_AEREA', DATE '1960-11-04', NULL, 'Veterano de prueba asociado a material fotografico.');

INSERT INTO actuaciones_veteranos (
    id,
    veterano_id,
    rango,
    unidad,
    rol,
    fecha_inicio,
    fecha_fin,
    descripcion
)
VALUES
    (1, 1, 'Cabo', 'Regimiento de Infanteria 25', 'Infante', DATE '1982-04-02', DATE '1982-06-14', 'Actuacion de prueba en operaciones terrestres.'),
    (2, 2, 'Marinero', 'ARA de prueba', 'Apoyo logistico', DATE '1982-04-10', DATE '1982-06-14', 'Actuacion de prueba vinculada a operaciones navales.'),
    (3, 3, 'Soldado', 'Base Aerea de prueba', 'Apoyo operativo', DATE '1982-04-15', DATE '1982-06-14', 'Actuacion de prueba vinculada a operaciones aereas.');

INSERT INTO objetos_museo (id, numero_inventario, nombre, tipo_objeto, descripcion)
VALUES
    (1, 'MM-DEV-0001', 'Carta enviada desde Puerto Argentino', 'Documento', 'Carta de prueba para validar carga, busqueda y asociacion con veteranos.'),
    (2, 'MM-DEV-0002', 'Fotografia de compania en campania', 'Fotografia', 'Fotografia de prueba para validar catalogacion visual.'),
    (3, 'MM-DEV-0003', 'Casco de campania', 'Indumentaria', 'Casco de prueba con marcas de uso.'),
    (4, 'MM-DEV-0004', 'Radio portatil de comunicaciones', 'Equipamiento', 'Equipo de radio de prueba para inventario y exhibicion.'),
    (5, 'MM-DEV-0005', 'Medalla conmemorativa', 'Reconocimiento', 'Medalla de prueba para categoria de reconocimientos.'),
    (6, 'MM-DEV-0006', 'Mapa operativo de las Islas Malvinas', 'Documento', 'Mapa de prueba para archivo documental.');

INSERT INTO objeto_categoria (id, objeto_museo_id, categoria_objeto_id, observaciones)
VALUES
    (1, 1, 1, 'Objeto sembrado para pruebas de documentos.'),
    (2, 1, 5, 'Tambien catalogado como objeto personal.'),
    (3, 2, 2, 'Objeto sembrado para pruebas de fotografias.'),
    (4, 3, 3, 'Objeto sembrado para pruebas de indumentaria.'),
    (5, 4, 4, 'Objeto sembrado para pruebas de equipamiento.'),
    (6, 5, 6, 'Objeto sembrado para pruebas de reconocimientos.'),
    (7, 6, 1, 'Objeto sembrado para pruebas de archivo documental.');

INSERT INTO objeto_depositante (
    id,
    objeto_museo_id,
    depositante_id,
    fecha_deposito,
    tipo_deposito,
    observaciones
)
VALUES
    (1, 1, 3, DATE '2024-03-15', 'Donacion', 'Carga inicial de desarrollo.'),
    (2, 2, 2, DATE '2024-04-02', 'Cesion', 'Carga inicial de desarrollo.'),
    (3, 3, 1, DATE '2024-04-20', 'Donacion', 'Carga inicial de desarrollo.'),
    (4, 4, 2, DATE '2024-05-05', 'Donacion', 'Carga inicial de desarrollo.'),
    (5, 5, 1, DATE '2024-05-18', 'Donacion', 'Carga inicial de desarrollo.'),
    (6, 6, 2, DATE '2024-06-01', 'Cesion', 'Carga inicial de desarrollo.');

INSERT INTO objeto_veterano (id, objeto_museo_id, veterano_id, tipo_relacion, descripcion)
VALUES
    (1, 1, 2, 'Autor referido', 'Carta asociada al veterano para pruebas de relacion.'),
    (2, 2, 3, 'Retratado', 'Fotografia asociada al veterano para pruebas de relacion.'),
    (3, 3, 1, 'Uso en servicio', 'Indumentaria asociada al veterano para pruebas de relacion.'),
    (4, 4, 1, 'Equipo vinculado', 'Equipamiento asociado al veterano para pruebas de relacion.');

INSERT INTO inventarios (
    id,
    objeto_museo_id,
    ubicacion_id,
    estado,
    estado_conservacion,
    fecha_ingreso,
    fecha_salida,
    fecha_ultimo_movimiento,
    observaciones
)
VALUES
    (1, 1, 5, 'DISPONIBLE', 'BUENO', DATE '2024-03-15', NULL, TIMESTAMP '2024-03-15 10:00:00', 'Ingreso inicial a archivo documental.'),
    (2, 2, 2, 'EN_EXHIBICION', 'BUENO', DATE '2024-04-02', NULL, TIMESTAMP '2024-07-01 09:30:00', 'Objeto incluido en exhibicion de prueba.'),
    (3, 3, 2, 'EN_EXHIBICION', 'REGULAR', DATE '2024-04-20', NULL, TIMESTAMP '2024-07-01 09:30:00', 'Objeto incluido en exhibicion de prueba.'),
    (4, 4, 4, 'EN_RESTAURACION', 'REGULAR', DATE '2024-05-05', NULL, TIMESTAMP '2024-06-10 11:15:00', 'En revision tecnica.'),
    (5, 5, 1, 'DISPONIBLE', 'EXCELENTE', DATE '2024-05-18', NULL, TIMESTAMP '2024-05-18 14:20:00', 'Disponible en deposito principal.'),
    (6, 6, 5, 'DISPONIBLE', 'BUENO', DATE '2024-06-01', NULL, TIMESTAMP '2024-06-01 16:40:00', 'Disponible en archivo documental.');

INSERT INTO movimientos_inventario (
    id,
    objeto_museo_id,
    tipo,
    fecha,
    ubicacion_origen_id,
    ubicacion_destino_id,
    usuario_id,
    observaciones
)
VALUES
    (1, 1, 'INGRESO', TIMESTAMP '2024-03-15 10:00:00', NULL, 5, NULL, 'Movimiento inicial sembrado para desarrollo.'),
    (2, 2, 'INGRESO', TIMESTAMP '2024-04-02 09:00:00', NULL, 1, NULL, 'Movimiento inicial sembrado para desarrollo.'),
    (3, 2, 'SALIDA_EXHIBICION', TIMESTAMP '2024-07-01 09:30:00', 1, 2, NULL, 'Salida a exhibicion de prueba.'),
    (4, 3, 'INGRESO', TIMESTAMP '2024-04-20 12:10:00', NULL, 1, NULL, 'Movimiento inicial sembrado para desarrollo.'),
    (5, 3, 'SALIDA_EXHIBICION', TIMESTAMP '2024-07-01 09:30:00', 1, 2, NULL, 'Salida a exhibicion de prueba.'),
    (6, 4, 'INGRESO', TIMESTAMP '2024-05-05 10:45:00', NULL, 1, NULL, 'Movimiento inicial sembrado para desarrollo.'),
    (7, 4, 'RESTAURACION', TIMESTAMP '2024-06-10 11:15:00', 1, 4, NULL, 'Derivado a taller de conservacion.'),
    (8, 5, 'INGRESO', TIMESTAMP '2024-05-18 14:20:00', NULL, 1, NULL, 'Movimiento inicial sembrado para desarrollo.'),
    (9, 6, 'INGRESO', TIMESTAMP '2024-06-01 16:40:00', NULL, 5, NULL, 'Movimiento inicial sembrado para desarrollo.');

INSERT INTO exhibiciones (id, nombre, descripcion, tipo, fecha_inicio, fecha_fin, estado)
VALUES
    (1, 'Malvinas: memorias en objetos', 'Exhibicion de prueba para validar alta, consulta e inclusion de objetos desde Swagger.', 'TEMPORAL', DATE '2024-07-01', DATE '2024-12-31', 'ACTIVA');

INSERT INTO exhibicion_objeto (
    id,
    exhibicion_id,
    objeto_museo_id,
    fecha_inclusion,
    fecha_retiro,
    estado,
    devolucion_verificada,
    verificado_por_usuario_id,
    fecha_verificacion,
    observaciones_devolucion
)
VALUES
    (1, 1, 2, DATE '2024-07-01', NULL, 'EN_EXHIBICION', FALSE, NULL, NULL, NULL),
    (2, 1, 3, DATE '2024-07-01', NULL, 'EN_EXHIBICION', FALSE, NULL, NULL, NULL);

SELECT setval(pg_get_serial_sequence('categoria_objeto', 'id'), (SELECT MAX(id) FROM categoria_objeto), TRUE);
SELECT setval(pg_get_serial_sequence('ubicaciones', 'id'), (SELECT MAX(id) FROM ubicaciones), TRUE);
SELECT setval(pg_get_serial_sequence('depositantes', 'id'), (SELECT MAX(id) FROM depositantes), TRUE);
SELECT setval(pg_get_serial_sequence('veteranos', 'id'), (SELECT MAX(id) FROM veteranos), TRUE);
SELECT setval(pg_get_serial_sequence('actuaciones_veteranos', 'id'), (SELECT MAX(id) FROM actuaciones_veteranos), TRUE);
SELECT setval(pg_get_serial_sequence('objetos_museo', 'id'), (SELECT MAX(id) FROM objetos_museo), TRUE);
SELECT setval(pg_get_serial_sequence('objeto_categoria', 'id'), (SELECT MAX(id) FROM objeto_categoria), TRUE);
SELECT setval(pg_get_serial_sequence('objeto_depositante', 'id'), (SELECT MAX(id) FROM objeto_depositante), TRUE);
SELECT setval(pg_get_serial_sequence('objeto_veterano', 'id'), (SELECT MAX(id) FROM objeto_veterano), TRUE);
SELECT setval(pg_get_serial_sequence('inventarios', 'id'), (SELECT MAX(id) FROM inventarios), TRUE);
SELECT setval(pg_get_serial_sequence('movimientos_inventario', 'id'), (SELECT MAX(id) FROM movimientos_inventario), TRUE);
SELECT setval(pg_get_serial_sequence('exhibiciones', 'id'), (SELECT MAX(id) FROM exhibiciones), TRUE);
SELECT setval(pg_get_serial_sequence('exhibicion_objeto', 'id'), (SELECT MAX(id) FROM exhibicion_objeto), TRUE);
