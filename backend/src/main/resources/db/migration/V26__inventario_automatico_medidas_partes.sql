ALTER TABLE objetos_museo
    ALTER COLUMN descripcion TYPE TEXT,
    ADD COLUMN medidas TEXT,
    ADD COLUMN cantidad_partes INTEGER NOT NULL DEFAULT 0;

UPDATE objetos_museo
SET medidas = concat_ws(E'\n',
    CASE WHEN nullif(btrim(alto), '') IS NOT NULL THEN 'Alto: ' || alto END,
    CASE WHEN nullif(btrim(ancho), '') IS NOT NULL THEN 'Ancho: ' || ancho END,
    CASE WHEN nullif(btrim(diametro), '') IS NOT NULL THEN 'Diámetro: ' || diametro END,
    CASE WHEN nullif(btrim(espesor), '') IS NOT NULL THEN 'Espesor: ' || espesor END,
    CASE WHEN nullif(btrim(peso), '') IS NOT NULL THEN 'Peso: ' || peso END
)
WHERE medidas IS NULL
  AND (nullif(btrim(alto), '') IS NOT NULL OR nullif(btrim(ancho), '') IS NOT NULL
    OR nullif(btrim(diametro), '') IS NOT NULL OR nullif(btrim(espesor), '') IS NOT NULL
    OR nullif(btrim(peso), '') IS NOT NULL);

ALTER TABLE objetos_museo ADD CONSTRAINT ck_objetos_museo_cantidad_partes_no_negativa CHECK (cantidad_partes >= 0);

CREATE TABLE correlativos_inventario (
    anio INTEGER PRIMARY KEY,
    ultimo INTEGER NOT NULL,
    CONSTRAINT ck_correlativos_inventario_rango CHECK (ultimo BETWEEN 1 AND 99999)
);

INSERT INTO correlativos_inventario (anio, ultimo)
SELECT substring(numero_inventario FROM 5 FOR 4)::INTEGER,
       max(substring(numero_inventario FROM 9 FOR 5)::INTEGER)
FROM objetos_museo
WHERE numero_inventario ~ '^MMAS[0-9]{9}$'
GROUP BY substring(numero_inventario FROM 5 FOR 4)::INTEGER
ON CONFLICT (anio) DO UPDATE SET ultimo = greatest(correlativos_inventario.ultimo, EXCLUDED.ultimo);
