CREATE INDEX IF NOT EXISTS idx_exhibicion_fecha_inicio ON exhibiciones (fecha_inicio);
CREATE INDEX IF NOT EXISTS idx_exhibicion_fecha_fin ON exhibiciones (fecha_fin);
CREATE INDEX IF NOT EXISTS idx_exhibicion_objeto_objeto ON exhibicion_objeto (objeto_museo_id);
