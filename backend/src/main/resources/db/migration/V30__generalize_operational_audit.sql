ALTER TABLE auditorias
    ALTER COLUMN entidad_id DROP NOT NULL,
    ADD COLUMN referencia_externa VARCHAR(160);

ALTER TABLE auditorias
    ADD CONSTRAINT ck_auditoria_referencia_entidad
    CHECK (entidad_id IS NOT NULL OR referencia_externa IS NOT NULL);

CREATE INDEX idx_auditoria_referencia_externa
    ON auditorias (entidad, referencia_externa, fecha DESC)
    WHERE referencia_externa IS NOT NULL;
