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
    void flywayAplicaMigracionesV1YV2EnPostgreSQL() {
        List<String> versiones = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success = true order by installed_rank",
                String.class
        );

        Integer objetosSembrados = jdbcTemplate.queryForObject("select count(*) from objetos_museo", Integer.class);
        Integer ubicacionesSembradas = jdbcTemplate.queryForObject("select count(*) from ubicaciones", Integer.class);
        Integer exhibicionesSembradas = jdbcTemplate.queryForObject("select count(*) from exhibiciones", Integer.class);

        assertThat(versiones).containsExactly("1", "2");
        assertThat(objetosSembrados).isGreaterThanOrEqualTo(6);
        assertThat(ubicacionesSembradas).isGreaterThanOrEqualTo(5);
        assertThat(exhibicionesSembradas).isGreaterThanOrEqualTo(1);
    }
}
