-- Fixture exclusivo del perfil de test. Nunca forma parte del artefacto productivo.
INSERT INTO depositantes (id, nombre, tipo, contacto, observaciones, activo, eliminado)
VALUES (1, 'Depositante fixture integración', 'PERSONA', 'fixture@integration.invalid',
        'Dato técnico aislado para builders de tests.', TRUE, FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO veteranos (
    id, nombre, apellido, fuerza, fecha_nacimiento, fecha_fallecimiento,
    historia, activo, eliminado
)
VALUES (
    1, 'Veterano', 'Fixture integración', 'EJERCITO', DATE '1960-01-01', NULL,
    'Dato técnico aislado para pruebas de actuaciones y catálogos.', TRUE, FALSE
)
ON CONFLICT (id) DO NOTHING;
