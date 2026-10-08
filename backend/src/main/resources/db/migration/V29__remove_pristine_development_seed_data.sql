-- V2 mezcló catálogos estructurales con un conjunto demo. Los catálogos se conservan.
-- El bloque elimina el grafo demo únicamente cuando conserva su huella completa y no
-- tiene referencias agregadas por usuarios. Ante cualquier divergencia, preserva todo.
DO $$
DECLARE
    demo_intacto BOOLEAN;
BEGIN
    SELECT
        (SELECT count(*) FROM objetos_museo
         WHERE (id, numero_inventario) IN (
             (1, 'MM-DEV-0001'), (2, 'MM-DEV-0002'), (3, 'MM-DEV-0003'),
             (4, 'MM-DEV-0004'), (5, 'MM-DEV-0005'), (6, 'MM-DEV-0006')
         )) = 6
        AND (SELECT count(*) FROM objeto_categoria WHERE objeto_museo_id BETWEEN 1 AND 6) = 7
        AND (SELECT count(*) FROM objeto_depositante WHERE objeto_museo_id BETWEEN 1 AND 6) = 6
        AND (SELECT count(*) FROM objeto_veterano WHERE objeto_museo_id BETWEEN 1 AND 6) = 4
        AND (SELECT count(*) FROM inventarios WHERE objeto_museo_id BETWEEN 1 AND 6) = 6
        AND (SELECT count(*) FROM movimientos_inventario WHERE objeto_museo_id BETWEEN 1 AND 6) = 9
        AND (SELECT count(*) FROM exhibicion_objeto WHERE objeto_museo_id BETWEEN 1 AND 6) = 2
        AND EXISTS (
            SELECT 1 FROM exhibiciones
            WHERE id = 1
              AND nombre = 'Malvinas: memorias en objetos'
              AND descripcion = 'Exhibicion de prueba para validar alta, consulta e inclusion de objetos desde Swagger.'
        )
        AND (SELECT count(*) FROM depositantes
             WHERE id IN (1, 2, 3) AND observaciones ILIKE '%prueba%') = 3
        AND (SELECT count(*) FROM veteranos
             WHERE id IN (1, 2, 3) AND historia ILIKE '%prueba%') = 3
        AND (SELECT count(*) FROM actuaciones_veteranos
             WHERE veterano_id IN (1, 2, 3) AND descripcion ILIKE '%prueba%') = 3
        AND NOT EXISTS (SELECT 1 FROM objetos_digitales WHERE id BETWEEN 1 AND 6)
        AND NOT EXISTS (SELECT 1 FROM fotos_objeto_museo WHERE objeto_museo_id BETWEEN 1 AND 6)
        AND NOT EXISTS (SELECT 1 FROM recibos_ingreso_objeto WHERE objeto_museo_id BETWEEN 1 AND 6)
        AND NOT EXISTS (SELECT 1 FROM recibos_escaneados_objeto_museo WHERE objeto_museo_id BETWEEN 1 AND 6)
        AND NOT EXISTS (SELECT 1 FROM relaciones_objetos WHERE objeto_origen_id BETWEEN 1 AND 6 OR objeto_destino_id BETWEEN 1 AND 6)
        AND NOT EXISTS (SELECT 1 FROM objeto_museo_detalles_conservacion WHERE objeto_museo_id BETWEEN 1 AND 6)
        AND NOT EXISTS (SELECT 1 FROM objeto_museo_visibilidades WHERE objeto_museo_id BETWEEN 1 AND 6)
        AND NOT EXISTS (SELECT 1 FROM embargos_objeto WHERE objeto_museo_id BETWEEN 1 AND 6)
        AND NOT EXISTS (SELECT 1 FROM auditorias WHERE entidad = 'OBJETO_MUSEO' AND entidad_id BETWEEN 1 AND 6)
        AND NOT EXISTS (SELECT 1 FROM objetos_museo WHERE id BETWEEN 1 AND 6 AND coleccion_id IS NOT NULL)
        AND NOT EXISTS (SELECT 1 FROM objeto_depositante WHERE depositante_id IN (1, 2, 3) AND objeto_museo_id NOT BETWEEN 1 AND 6)
        AND NOT EXISTS (SELECT 1 FROM objeto_veterano WHERE veterano_id IN (1, 2, 3) AND objeto_museo_id NOT BETWEEN 1 AND 6)
        AND NOT EXISTS (SELECT 1 FROM veterano_imagen WHERE veterano_id IN (1, 2, 3))
        AND NOT EXISTS (SELECT 1 FROM veterano_video WHERE veterano_id IN (1, 2, 3))
    INTO demo_intacto;

    IF demo_intacto THEN
        DELETE FROM exhibicion_objeto WHERE exhibicion_id = 1 AND objeto_museo_id IN (2, 3);
        DELETE FROM exhibiciones WHERE id = 1 AND nombre = 'Malvinas: memorias en objetos';
        DELETE FROM movimientos_inventario WHERE objeto_museo_id BETWEEN 1 AND 6;
        DELETE FROM inventarios WHERE objeto_museo_id BETWEEN 1 AND 6;
        DELETE FROM objeto_categoria WHERE objeto_museo_id BETWEEN 1 AND 6;
        DELETE FROM objeto_depositante WHERE objeto_museo_id BETWEEN 1 AND 6;
        DELETE FROM objeto_veterano WHERE objeto_museo_id BETWEEN 1 AND 6;
        DELETE FROM objetos_museo WHERE id BETWEEN 1 AND 6 AND numero_inventario LIKE 'MM-DEV-%';
        DELETE FROM actuaciones_veteranos WHERE veterano_id IN (1, 2, 3) AND descripcion ILIKE '%prueba%';
        DELETE FROM veteranos WHERE id IN (1, 2, 3) AND historia ILIKE '%prueba%';
        DELETE FROM depositantes WHERE id IN (1, 2, 3) AND observaciones ILIKE '%prueba%';
        RAISE NOTICE 'Datos demo históricos V2 eliminados; catálogos estructurales conservados.';
    ELSE
        RAISE WARNING 'Datos demo V2 modificados o referenciados: se preservan para evitar pérdida de historia. Revisión manual requerida.';
    END IF;
END $$;
