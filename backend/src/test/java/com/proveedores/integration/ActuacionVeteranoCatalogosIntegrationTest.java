package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.dto.ActuacionVeteranoRequestDTO;
import com.proveedores.entity.Fuerza;
import com.proveedores.exception.BusinessException;
import com.proveedores.repository.RangoMilitarRepository;
import com.proveedores.repository.UnidadMilitarRepository;
import com.proveedores.service.ActuacionVeteranoService;
import com.proveedores.service.RangoMilitarService;
import com.proveedores.service.UnidadMilitarService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ActuacionVeteranoCatalogosIntegrationTest extends IntegrationTestBase {

    @Autowired
    private RangoMilitarService rangoMilitarService;

    @Autowired
    private UnidadMilitarService unidadMilitarService;

    @Autowired
    private ActuacionVeteranoService actuacionVeteranoService;

    @Autowired
    private RangoMilitarRepository rangoMilitarRepository;

    @Autowired
    private UnidadMilitarRepository unidadMilitarRepository;

    @Test
    void listaRangosActivosPorFuerzaOrdenadosPorJerarquia() {
        var rangos = rangoMilitarService.listarPorFuerza(Fuerza.EJERCITO);

        assertThat(rangos).isNotEmpty();
        assertThat(rangos).allSatisfy(rango -> assertThat(rango.fuerza()).isEqualTo(Fuerza.EJERCITO));
        assertThat(rangos).extracting("ordenJerarquico").isSorted();
        assertThat(rangos).extracting("nombre").containsExactly(
                "Teniente General",
                "General de División",
                "General de Brigada",
                "Coronel Mayor",
                "Coronel",
                "Teniente Coronel",
                "Mayor",
                "Capitán",
                "Teniente Primero",
                "Teniente",
                "Subteniente",
                "Suboficial Mayor",
                "Suboficial Principal",
                "Sargento Ayudante",
                "Sargento Primero",
                "Sargento",
                "Cabo Primero",
                "Cabo"
        );
    }

    @Test
    void buscaUnidadesPorFuerzaYTextoCaseInsensitive() {
        var unidades = unidadMilitarService.buscarPorFuerza(Fuerza.EJERCITO, "regimiento", 20);
        var porSigla = unidadMilitarService.buscarPorFuerza(Fuerza.EJERCITO, "ri 25", 20);
        var otraFuerza = unidadMilitarService.buscarPorFuerza(Fuerza.ARMADA, "ri 25", 20);

        assertThat(unidades).extracting("nombre").anyMatch(nombre -> nombre.toString().contains("Regimiento"));
        assertThat(porSigla).extracting("sigla").contains("RI 25");
        assertThat(otraFuerza).isEmpty();
    }

    @Test
    void creaActuacionConRangoYUnidadValidos() {
        var rango = rangoMilitarRepository.findByFuerzaAndActivoTrueAndEliminadoFalseOrderByOrdenJerarquicoAsc(Fuerza.EJERCITO).get(0);
        var unidad = unidadMilitarRepository.buscarActivasPorFuerza(Fuerza.EJERCITO, "RI 25", org.springframework.data.domain.PageRequest.of(0, 1)).get(0);

        var actuacion = actuacionVeteranoService.crear(new ActuacionVeteranoRequestDTO(
                1L,
                null,
                null,
                rango.getId(),
                unidad.getId(),
                "Infante",
                LocalDate.of(1982, 4, 2),
                LocalDate.of(1982, 6, 14),
                "Actuacion con catalogos"
        ));

        assertThat(actuacion.rangoId()).isEqualTo(rango.getId());
        assertThat(actuacion.rangoNombre()).isEqualTo(rango.getNombre());
        assertThat(actuacion.unidadId()).isEqualTo(unidad.getId());
        assertThat(actuacion.unidadNombre()).isEqualTo(unidad.getNombre());
        assertThat(actuacion.unidadSigla()).isEqualTo(unidad.getSigla());
        assertThat(actuacion.rango()).isEqualTo(rango.getNombre());
        assertThat(actuacion.unidad()).isEqualTo(unidad.getNombre());
    }

    @Test
    void rechazaRangoIncompatibleConFuerzaDelVeterano() {
        var rangoArmada = rangoMilitarRepository.findByFuerzaAndActivoTrueAndEliminadoFalseOrderByOrdenJerarquicoAsc(Fuerza.ARMADA).get(0);

        assertThatThrownBy(() -> actuacionVeteranoService.crear(new ActuacionVeteranoRequestDTO(
                1L,
                null,
                null,
                rangoArmada.getId(),
                null,
                "Rol",
                null,
                null,
                null
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El rango seleccionado no pertenece a la fuerza del veterano");
    }

    @Test
    void rechazaUnidadIncompatibleConFuerzaDelVeterano() {
        var unidadArmada = unidadMilitarRepository.buscarActivasPorFuerza(Fuerza.ARMADA, "BIM", org.springframework.data.domain.PageRequest.of(0, 1)).get(0);

        assertThatThrownBy(() -> actuacionVeteranoService.crear(new ActuacionVeteranoRequestDTO(
                1L,
                null,
                null,
                null,
                unidadArmada.getId(),
                "Rol",
                null,
                null,
                null
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("La unidad seleccionada no pertenece a la fuerza del veterano");
    }

    @Test
    void actualizaActuacionConRangoYUnidadValidos() {
        var rango = rangoMilitarRepository.findByFuerzaAndActivoTrueAndEliminadoFalseOrderByOrdenJerarquicoAsc(Fuerza.EJERCITO).get(0);
        var unidad = unidadMilitarRepository.buscarActivasPorFuerza(Fuerza.EJERCITO, "RI 25", org.springframework.data.domain.PageRequest.of(0, 1)).get(0);
        var actuacion = actuacionVeteranoService.crear(new ActuacionVeteranoRequestDTO(
                1L,
                "Rango legacy",
                "Unidad legacy",
                null,
                null,
                "Rol",
                null,
                null,
                null
        ));

        var actualizada = actuacionVeteranoService.actualizar(actuacion.id(), new ActuacionVeteranoRequestDTO(
                1L,
                null,
                null,
                rango.getId(),
                unidad.getId(),
                "Rol actualizado",
                null,
                null,
                null
        ));

        assertThat(actualizada.rangoId()).isEqualTo(rango.getId());
        assertThat(actualizada.unidadId()).isEqualTo(unidad.getId());
        assertThat(actualizada.rol()).isEqualTo("Rol actualizado");
    }
}
