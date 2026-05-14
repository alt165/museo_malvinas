ALTER TABLE depositantes
    ADD COLUMN dni VARCHAR(30),
    ADD COLUMN cuit VARCHAR(30);

CREATE INDEX idx_depositantes_dni ON depositantes (dni);
CREATE INDEX idx_depositantes_cuit ON depositantes (cuit);

CREATE UNIQUE INDEX uk_depositantes_dni_normalizado
    ON depositantes (regexp_replace(dni, '[^0-9]', '', 'g'))
    WHERE dni IS NOT NULL AND regexp_replace(dni, '[^0-9]', '', 'g') <> '';

CREATE UNIQUE INDEX uk_depositantes_cuit_normalizado
    ON depositantes (regexp_replace(cuit, '[^0-9]', '', 'g'))
    WHERE cuit IS NOT NULL AND regexp_replace(cuit, '[^0-9]', '', 'g') <> '';
