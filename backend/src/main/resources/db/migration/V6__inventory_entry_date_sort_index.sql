CREATE INDEX idx_inventarios_fecha_ingreso ON inventarios (fecha_ingreso);
CREATE INDEX idx_inventarios_objeto_museo_fecha_ingreso ON inventarios (objeto_museo_id, fecha_ingreso);
