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

        assertThat(versiones).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20");
        assertThat(objetosSembrados).isGreaterThanOrEqualTo(6);
        assertThat(ubicacionesSembradas).isGreaterThanOrEqualTo(5);
        assertThat(jdbcTemplate.queryForObject("select count(*) from ubicaciones where nombre = 'Pre ingreso' and eliminado = false", Integer.class))
                .isEqualTo(1);
        assertThat(exhibicionesSembradas).isGreaterThanOrEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from pg_indexes where tablename = 'exhibiciones' and indexname in ('idx_exhibicion_fecha_inicio', 'idx_exhibicion_fecha_fin')", Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from pg_indexes where tablename = 'exhibicion_objeto' and indexname = 'idx_exhibicion_objeto_objeto'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.tables where table_name = 'fotos_objeto_museo'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.tables where table_name = 'recibos_ingreso_objeto'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = 'objetos_museo' and column_name in ('nombre', 'tipo_objeto')", Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = 'ubicaciones' and column_name = 'tipo'", Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = 'objetos_museo' and column_name = 'eliminado_por'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from pg_indexes where tablename = 'objetos_museo' and indexname in ('idx_objetos_museo_eliminado', 'idx_objetos_museo_fecha_eliminacion')", Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from pg_indexes where tablename = 'inventarios' and indexname in ('idx_inventarios_fecha_ingreso', 'idx_inventarios_objeto_museo_fecha_ingreso')", Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = 'depositantes' and column_name in ('dni', 'cuit') and data_type = 'character varying'", Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from pg_indexes where tablename = 'depositantes' and indexname in ('idx_depositantes_dni', 'idx_depositantes_cuit', 'uk_depositantes_dni_normalizado', 'uk_depositantes_cuit_normalizado')", Integer.class))
                .isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = 'objetos_museo' and column_name in ('origen_carga', 'datos_completos', 'fecha_carga_rapida', 'carga_rapida_por')", Integer.class))
                .isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("select count(*) from pg_indexes where tablename = 'objetos_museo' and indexname in ('idx_objetos_museo_pendientes_completar', 'idx_objetos_museo_fecha_carga_rapida')", Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.tables where table_name = 'recibos_escaneados_objeto_museo'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = 'fotos_objeto_museo' and column_name in ('nombre_archivo_original', 'nombre_archivo_almacenado', 'ruta_relativa')", Integer.class))
                .isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("select count(*) from pg_indexes where tablename = 'recibos_escaneados_objeto_museo' and indexname in ('idx_recibos_escaneados_objeto', 'uk_recibo_escaneado_activo_objeto')", Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.tables where table_name = 'colecciones_objetos'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = 'objetos_museo' and column_name = 'coleccion_id'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from pg_indexes where tablename = 'objetos_museo' and indexname = 'idx_objetos_museo_coleccion'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from pg_indexes where tablename = 'colecciones_objetos' and indexname = 'uk_colecciones_objetos_nombre_activo'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = 'relaciones_objetos' and column_name in ('fecha_creacion', 'creado_por')", Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from pg_indexes where tablename = 'relaciones_objetos' and indexname in ('idx_relacion_objeto_origen', 'idx_relacion_objeto_destino', 'idx_relacion_objeto_origen_destino', 'idx_relacion_objeto_tipo', 'uk_relacion_objeto_direccional_activa')", Integer.class))
                .isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.tables where table_name = 'configuracion_sistema'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select valor from configuracion_sistema where clave = 'comodatos_prestamos.dias_alerta'", String.class))
                .isEqualTo("14");
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = 'auditorias' and column_name in ('accion', 'descripcion', 'origen', 'numero_inventario', 'usuario_identificador', 'usuario_nombre', 'rol')", Integer.class))
                .isEqualTo(7);
        assertThat(jdbcTemplate.queryForObject("select count(*) from pg_indexes where tablename = 'auditorias' and indexname = 'idx_auditoria_objeto_fecha'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.tables where table_name in ('rangos_militares', 'unidades_militares')", Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_name = 'actuaciones_veteranos' and column_name in ('rango_id', 'unidad_id')", Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from rangos_militares where activo = true", Integer.class))
                .isGreaterThan(0);
        assertThat(jdbcTemplate.queryForObject("select count(*) from unidades_militares where activo = true", Integer.class))
                .isGreaterThan(0);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.tables where table_name in ('veterano_imagen', 'veterano_video')", Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from pg_indexes where tablename = 'veterano_imagen' and indexname in ('idx_veterano_imagen_veterano', 'idx_veterano_imagen_activo', 'idx_veterano_imagen_orden')", Integer.class))
                .isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("select count(*) from pg_indexes where tablename = 'veterano_video' and indexname in ('idx_veterano_video_veterano', 'idx_veterano_video_activo', 'idx_veterano_video_orden')", Integer.class))
                .isEqualTo(3);
    }
}
