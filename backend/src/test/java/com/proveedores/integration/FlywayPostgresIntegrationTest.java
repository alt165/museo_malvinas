package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class FlywayPostgresIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAplicaMigracionesEnPostgreSQL() {
        List<String> versiones = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success = true order by installed_rank",
                String.class
        );

        Integer objetosSembrados = jdbcTemplate.queryForObject("select count(*) from objetos_museo", Integer.class);
        Integer ubicacionesSembradas = jdbcTemplate.queryForObject("select count(*) from ubicaciones", Integer.class);
        Integer exhibicionesSembradas = jdbcTemplate.queryForObject("select count(*) from exhibiciones", Integer.class);

        assertThat(versiones).containsExactly("1", "2", "3", "4", "5", "6");
        assertThat(objetosSembrados).isGreaterThanOrEqualTo(6);
        assertThat(ubicacionesSembradas).isGreaterThanOrEqualTo(5);
        assertThat(exhibicionesSembradas).isGreaterThanOrEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.tables where table_name = 'fotos_objeto_museo'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.tables where table_name = 'recibos_ingreso_objeto'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = 'objetos_museo' and column_name in ('nombre', 'tipo_objeto')", Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = 'objetos_museo' and column_name = 'eliminado_por'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from pg_indexes where tablename = 'objetos_museo' and indexname in ('idx_objetos_museo_eliminado', 'idx_objetos_museo_fecha_eliminacion')", Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from pg_indexes where tablename = 'inventarios' and indexname in ('idx_inventarios_fecha_ingreso', 'idx_inventarios_objeto_museo_fecha_ingreso')", Integer.class))
                .isEqualTo(2);
    }
}
