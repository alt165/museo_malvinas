DROP INDEX IF EXISTS idx_objeto_museo_nombre;

ALTER TABLE objetos_museo
    DROP COLUMN IF EXISTS nombre,
    DROP COLUMN IF EXISTS tipo_objeto;
