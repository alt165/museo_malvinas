package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.dto.InventarioRequestDTO;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.EstadoConservacion;
import com.proveedores.entity.EstadoInventario;
import com.proveedores.entity.TipoMovimientoInventario;
import com.proveedores.repository.InventarioRepository;
import com.proveedores.repository.MovimientoInventarioRepository;
import com.proveedores.service.InventarioService;
import com.proveedores.service.ObjetoMuseoService;
import com.proveedores.testfixture.ObjetoMuseoTestFixture;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InventarioServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private ObjetoMuseoService objetoMuseoService;

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Test
    void creaInventarioYRegistraMovimientoDeIngreso() {
        var objeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-INV-001", "Linterna de campania"));

        var response = inventarioService.crear(new InventarioRequestDTO(
                objeto.id(),
                1L,
                EstadoInventario.DISPONIBLE,
                EstadoConservacion.BUENO,
                LocalDate.now(),
                null,
                "Ingreso por test de integracion"
        ));

        assertThat(inventarioRepository.findById(response.id()))
                .get()
                .satisfies(inventario -> {
                    assertThat(inventario.getObjetoMuseo().getId()).isEqualTo(objeto.id());
                    assertThat(inventario.getUbicacion().getId()).isEqualTo(1L);
                    assertThat(inventario.getEstado()).isEqualTo(EstadoInventario.DISPONIBLE);
                    assertThat(inventario.getFechaUltimoMovimiento()).isNotNull();
                });

        assertThat(movimientoInventarioRepository.findByObjetoMuseoIdAndEliminadoFalseOrderByFechaDesc(objeto.id()))
                .singleElement()
                .satisfies(movimiento -> {
                    assertThat(movimiento.getTipo()).isEqualTo(TipoMovimientoInventario.INGRESO);
                    assertThat(movimiento.getUbicacionOrigen()).isNull();
                    assertThat(movimiento.getUbicacionDestino().getId()).isEqualTo(1L);
                });
    }

    @Test
    void recrearInventarioEliminadoReactivaElMismoRegistro() {
        var objeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-INV-REACT", "Inventario reactivable"));
        var request = new InventarioRequestDTO(objeto.id(), 1L, EstadoInventario.DISPONIBLE,
                EstadoConservacion.BUENO, LocalDate.now(), null, "Inicial");
        var creado = inventarioService.crear(request);
        inventarioService.bajaLogica(creado.id());

        var reactivado = inventarioService.crear(new InventarioRequestDTO(objeto.id(), 1L,
                EstadoInventario.DISPONIBLE, EstadoConservacion.BUENO, LocalDate.now(), null, "Reactivado"));

        assertThat(reactivado.id()).isEqualTo(creado.id());
        assertThat(inventarioRepository.findById(creado.id())).get()
                .satisfies(inventario -> {
                    assertThat(inventario.getActivo()).isTrue();
                    assertThat(inventario.getEliminado()).isFalse();
                });
    }

    @Test
    void inventarioNoPuedeCambiarDeObjeto() {
        var objetoOriginal = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-INV-IMM-1", "Objeto original"));
        var otroObjeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-INV-IMM-2", "Otro objeto"));
        var inventario = inventarioService.crear(new InventarioRequestDTO(
                objetoOriginal.id(), 1L, EstadoInventario.DISPONIBLE, EstadoConservacion.BUENO,
                LocalDate.now(), null, "Identidad fija"
        ));

        assertThatThrownBy(() -> inventarioService.actualizar(inventario.id(), new InventarioRequestDTO(
                otroObjeto.id(), 2L, EstadoInventario.DISPONIBLE, EstadoConservacion.BUENO,
                LocalDate.now(), null, "Intento inválido"
        ))).isInstanceOf(com.proveedores.exception.ConflictException.class);

        assertThat(inventarioRepository.findById(inventario.id())).get()
                .satisfies(actual -> assertThat(actual.getObjetoMuseo().getId()).isEqualTo(objetoOriginal.id()));
    }

    @Test
    void correccionDeUbicacionAgregaMovimientosSinReescribirElOriginal() {
        var objeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-INV-APPEND", "Objeto con historial"));
        var inventario = inventarioService.crear(new InventarioRequestDTO(
                objeto.id(), 1L, EstadoInventario.DISPONIBLE, EstadoConservacion.BUENO,
                LocalDate.now(), null, "Ingreso"
        ));
        inventarioService.actualizar(inventario.id(), new InventarioRequestDTO(
                objeto.id(), 2L, EstadoInventario.DISPONIBLE, EstadoConservacion.BUENO,
                LocalDate.now(), null, "Movimiento informado por error"
        ));
        inventarioService.actualizar(inventario.id(), new InventarioRequestDTO(
                objeto.id(), 1L, EstadoInventario.DISPONIBLE, EstadoConservacion.BUENO,
                LocalDate.now(), null, "Movimiento compensatorio"
        ));

        var historial = movimientoInventarioRepository.findByObjetoMuseoIdAndEliminadoFalseOrderByFechaDesc(objeto.id());
        assertThat(historial).hasSize(3);
        assertThat(historial).allSatisfy(movimiento -> {
            assertThat(movimiento.getEliminado()).isFalse();
            assertThat(movimiento.getFecha()).isNotNull();
        });
        assertThat(historial).extracting(movimiento -> movimiento.getTipo())
                .containsExactly(TipoMovimientoInventario.CAMBIO_UBICACION, TipoMovimientoInventario.CAMBIO_UBICACION, TipoMovimientoInventario.INGRESO);
        assertThat(historial.get(1).getUbicacionOrigen().getId()).isEqualTo(1L);
        assertThat(historial.get(1).getUbicacionDestino().getId()).isEqualTo(2L);
        assertThat(historial.get(0).getUbicacionOrigen().getId()).isEqualTo(2L);
        assertThat(historial.get(0).getUbicacionDestino().getId()).isEqualTo(1L);
    }
}
