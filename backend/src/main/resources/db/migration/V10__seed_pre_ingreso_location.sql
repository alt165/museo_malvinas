INSERT INTO ubicaciones (nombre, tipo, descripcion, activo, eliminado)
SELECT 'Pre ingreso', 'PRE_INGRESO', 'Ubicacion inicial asignada automaticamente a objetos creados por carga rapida.', TRUE, FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM ubicaciones
    WHERE LOWER(nombre) = LOWER('Pre ingreso')
      AND eliminado = FALSE
);
