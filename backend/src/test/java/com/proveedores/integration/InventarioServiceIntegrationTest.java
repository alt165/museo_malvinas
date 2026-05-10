package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.proveedores.dto.InventarioRequestDTO;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.entity.EstadoConservacion;
import com.proveedores.entity.EstadoInventario;
import com.proveedores.entity.TipoMovimientoInventario;
import com.proveedores.repository.InventarioRepository;
import com.proveedores.repository.MovimientoInventarioRepository;
import com.proveedores.service.InventarioService;
import com.proveedores.service.ObjetoMuseoService;
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
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-INV-001",
                "Linterna de campania",
                null,
                null, null, null, null, null
        ));

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
}
