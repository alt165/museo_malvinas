package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

class FlywayV28UpgradeIntegrationTest {

    @Test
    void actualizaV28ALatestSinDemoYConservaCatalogos() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")) {
            postgres.start();
            Flyway hastaV28 = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .target(MigrationVersion.fromVersion("28"))
                    .load();
            hastaV28.migrate();

            JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
            assertThat(jdbc.queryForObject(
                    "select count(*) from objetos_museo where numero_inventario like 'MM-DEV-%'", Integer.class))
                    .isEqualTo(6);

            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .load()
                    .migrate();

            assertThat(jdbc.queryForObject(
                    "select count(*) from objetos_museo where numero_inventario like 'MM-DEV-%'", Integer.class))
                    .isZero();
            assertThat(jdbc.queryForObject(
                    "select count(*) from categoria_objeto where eliminado = false", Integer.class))
                    .isGreaterThanOrEqualTo(6);
            assertThat(jdbc.queryForObject(
                    "select count(*) from flyway_schema_history where version = '30' and success", Integer.class))
                    .isEqualTo(1);
        }
    }

    @Test
    void preservaSeedSiFueUsadoComoDatoReal() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")) {
            postgres.start();
            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .target(MigrationVersion.fromVersion("28"))
                    .load()
                    .migrate();
            JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
            jdbc.update("update objetos_museo set descripcion = 'Dato histórico revisado' where id = 1");
            jdbc.update("insert into fotos_objeto_museo (objeto_museo_id, nombre_archivo, nombre_archivo_original, nombre_archivo_almacenado, ruta_almacenamiento, ruta_relativa, content_type, tamanio_bytes, visibilidad, fecha_carga, eliminado) values (1, 'evidencia.jpg', 'evidencia.jpg', 'seguro.jpg', 'objetos/1/seguro.jpg', 'objetos/1/seguro.jpg', 'image/jpeg', 4, 'PRIVADO', current_timestamp, false)");

            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .load()
                    .migrate();

            assertThat(jdbc.queryForObject(
                    "select count(*) from objetos_museo where numero_inventario like 'MM-DEV-%'", Integer.class))
                    .isEqualTo(6);
            assertThat(jdbc.queryForObject(
                    "select count(*) from fotos_objeto_museo where objeto_museo_id = 1", Integer.class))
                    .isEqualTo(1);
        }
    }
}
