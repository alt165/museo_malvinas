package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;
import com.proveedores.dto.ExhibicionObjetoRequestDTO;
import com.proveedores.dto.ExhibicionRequestDTO;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.EstadoExhibicionObjeto;
import com.proveedores.entity.TipoExhibicion;
import com.proveedores.repository.ExhibicionObjetoRepository;
import com.proveedores.repository.ExhibicionRepository;
import com.proveedores.service.ExhibicionObjetoService;
import com.proveedores.service.ExhibicionService;
import com.proveedores.service.ObjetoMuseoService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ExhibicionServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ExhibicionService exhibicionService;

    @Autowired
    private ExhibicionObjetoService exhibicionObjetoService;

    @Autowired
    private ObjetoMuseoService objetoMuseoService;

    @Autowired
    private ExhibicionRepository exhibicionRepository;

    @Autowired
    private ExhibicionObjetoRepository exhibicionObjetoRepository;

    @Test
    void finalizaExhibicionConObjetosPendientesYLosLibera() {
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-EXH-PEND",
                "Mapa de sala",
                null,
                null, null, null, null, null,
                null,
                1L,
                CaracterRecepcionObjeto.DONACION,
                null
        ));
        var exhibicion = crearExhibicion("IT Exhibicion pendiente");

        exhibicionObjetoService.crear(new ExhibicionObjetoRequestDTO(
                exhibicion.id(),
                objeto.id(),
                LocalDate.now(),
                null,
                EstadoExhibicionObjeto.EN_EXHIBICION,
                false,
                null,
                null,
                null
        ));

        var response = exhibicionService.finalizar(exhibicion.id());

        assertThat(response.estado()).isEqualTo(EstadoExhibicion.FINALIZADA);
        assertThat(exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(exhibicion.id()))
                .singleElement()
                .satisfies(relacion -> {
                    assertThat(relacion.getEstado()).isEqualTo(EstadoExhibicionObjeto.DEVUELTO);
                    assertThat(relacion.getDevolucionVerificada()).isTrue();
                    assertThat(relacion.getFechaRetiro()).isEqualTo(LocalDate.now());
                });
    }

    @Test
    void finalizaExhibicionCuandoTodosLosObjetosFueronDevueltos() {
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-EXH-FIN",
                "Panel fotografico",
                null,
                null, null, null, null, null,
                null,
                1L,
                CaracterRecepcionObjeto.DONACION,
                null
        ));
        var exhibicion = crearExhibicion("IT Exhibicion finalizable");
        var relacion = exhibicionObjetoService.crear(new ExhibicionObjetoRequestDTO(
                exhibicion.id(),
                objeto.id(),
                LocalDate.now(),
                null,
                EstadoExhibicionObjeto.EN_EXHIBICION,
                false,
                null,
                null,
                null
        ));

        exhibicionObjetoService.verificarDevolucion(relacion.id(), null, "Devuelto");

        var response = exhibicionService.finalizar(exhibicion.id());

        assertThat(response.estado()).isEqualTo(EstadoExhibicion.FINALIZADA);
        assertThat(exhibicionRepository.findById(exhibicion.id()))
                .get()
                .satisfies(entity -> assertThat(entity.getEstado()).isEqualTo(EstadoExhibicion.FINALIZADA));
    }

    @Test
    void objetoQuedaDisponibleParaNuevaExhibicionAlFinalizarElMismoDia() {
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-EXH-REUSE",
                "Pelota historica",
                null,
                null, null, null, null, null,
                null,
                1L,
                CaracterRecepcionObjeto.DONACION,
                null
        ));
        var exhibicion = crearExhibicion("IT Exhibicion finalizada hoy");

        exhibicionObjetoService.crear(new ExhibicionObjetoRequestDTO(
                exhibicion.id(),
                objeto.id(),
                LocalDate.now(),
                null,
                EstadoExhibicionObjeto.EN_EXHIBICION,
                false,
                null,
                null,
                null
        ));

        exhibicionService.finalizar(exhibicion.id());

        var disponibilidad = exhibicionService.buscarObjetosDisponibilidad(
                objeto.numeroInventario(),
                LocalDate.now(),
                null,
                null,
                org.springframework.data.domain.PageRequest.of(0, 10)
        );
        assertThat(disponibilidad.getContent())
                .extracting(com.proveedores.dto.ObjetoDisponibilidadExhibicionResponseDTO::disponible)
                .contains(true);

        var nueva = exhibicionService.crear(new ExhibicionRequestDTO(
                "IT Exhibicion reutiliza objeto",
                "Nueva exhibicion con objeto liberado",
                TipoExhibicion.PERMANENTE,
                LocalDate.now(),
                null,
                EstadoExhibicion.ACTIVA,
                java.util.Set.of(objeto.id())
        ));

        assertThat(nueva.objetos()).singleElement()
                .satisfies(relacion -> assertThat(relacion.objetoMuseoId()).isEqualTo(objeto.id()));
    }

    private com.proveedores.dto.ExhibicionResponseDTO crearExhibicion(String nombre) {
        return exhibicionService.crear(new ExhibicionRequestDTO(
                nombre,
                "Exhibicion generada por test de integracion",
                TipoExhibicion.TEMPORAL,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                EstadoExhibicion.ACTIVA
        ));
    }
}
