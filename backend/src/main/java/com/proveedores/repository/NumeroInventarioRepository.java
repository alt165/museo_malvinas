package com.proveedores.repository;

import com.proveedores.exception.BusinessException;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NumeroInventarioRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public NumeroInventarioRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int siguienteCorrelativo(int anio) {
        Integer correlativo;
        try {
            correlativo = jdbcTemplate.queryForObject("""
                INSERT INTO correlativos_inventario (anio, ultimo) VALUES (:anio, 1)
                ON CONFLICT (anio) DO UPDATE SET ultimo = correlativos_inventario.ultimo + 1
                WHERE correlativos_inventario.ultimo < 99999
                RETURNING ultimo
                """, Map.of("anio", anio), Integer.class);
        } catch (EmptyResultDataAccessException ex) {
            correlativo = null;
        }
        if (correlativo == null) {
            throw new BusinessException("Se agotaron los numeros de inventario disponibles para el ano " + anio);
        }
        return correlativo;
    }
}
