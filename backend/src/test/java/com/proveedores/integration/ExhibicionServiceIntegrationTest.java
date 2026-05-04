package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.dto.ExhibicionObjetoRequestDTO;
import com.proveedores.dto.ExhibicionRequestDTO;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.EstadoExhibicionObjeto;
import com.proveedores.entity.TipoExhibicion;
import com.proveedores.exception.BusinessException;
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

    @Test
    void noFinalizaExhibicionConObjetosPendientesDeDevolucion() {
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-EXH-PEND",
                "Mapa de sala",
                "Documento",
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

        assertThatThrownBy(() -> exhibicionService.finalizar(exhibicion.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("No se puede finalizar la exhibicion con objetos pendientes de devolucion");
    }

    @Test
    void finalizaExhibicionCuandoTodosLosObjetosFueronDevueltos() {
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-EXH-FIN",
                "Panel fotografico",
                "Fotografia",
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
