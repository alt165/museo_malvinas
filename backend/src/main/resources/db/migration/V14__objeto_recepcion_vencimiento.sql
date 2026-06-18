UPDATE objeto_depositante
SET tipo_deposito = CASE
    WHEN upper(tipo_deposito) IN ('PRESTAMO', 'PRÉSTAMO') THEN 'PRESTAMO'
    WHEN upper(tipo_deposito) = 'COMODATO' THEN 'COMODATO'
    WHEN upper(tipo_deposito) IN ('DONACION', 'DONACIÓN') THEN 'DONACION'
    WHEN upper(tipo_deposito) = 'COMPRA' THEN 'COMPRA'
    WHEN upper(tipo_deposito) = 'ESTUDIO' THEN 'ESTUDIO'
    WHEN upper(tipo_deposito) = 'OTRO' THEN 'OTRO'
    WHEN upper(tipo_deposito) IN ('RECEPCION', 'RECEPCIÓN') THEN 'RECEPCION'
    ELSE 'OTRO'
END
WHERE tipo_deposito IS NOT NULL;

ALTER TABLE objeto_depositante
    ADD COLUMN fecha_vencimiento DATE;

CREATE INDEX idx_objeto_depositante_fecha_vencimiento
    ON objeto_depositante (fecha_vencimiento);
