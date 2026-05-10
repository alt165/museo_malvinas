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
import com.proveedores.repository.ExhibicionObjetoRepository;
import com.proveedores.service.ExhibicionObjetoService;
import com.proveedores.service.ExhibicionService;
import com.proveedores.service.ObjetoMuseoService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ExhibicionObjetoServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ExhibicionObjetoService exhibicionObjetoService;

    @Autowired
    private ExhibicionService exhibicionService;

    @Autowired
    private ObjetoMuseoService objetoMuseoService;

    @Autowired
    private ExhibicionObjetoRepository exhibicionObjetoRepository;

    @Test
    void impideObjetoEnMasDeUnaExhibicionActiva() {
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-EXO-001",
                "Cantimplora",
                null,
                null, null, null, null, null
        ));
        var exhibicionActiva = crearExhibicion("IT Exhibicion activa A", EstadoExhibicion.ACTIVA);
        var otraExhibicionActiva = crearExhibicion("IT Exhibicion activa B", EstadoExhibicion.ACTIVA);

        exhibicionObjetoService.crear(request(exhibicionActiva.id(), objeto.id(), EstadoExhibicionObjeto.EN_EXHIBICION));

        assertThatThrownBy(() -> exhibicionObjetoService.crear(
                request(otraExhibicionActiva.id(), objeto.id(), EstadoExhibicionObjeto.EN_EXHIBICION)
        )).isInstanceOf(BusinessException.class)
                .hasMessage("El objeto ya esta asociado a una exhibicion activa");
    }

    @Test
    void verificaDevolucionEnPostgreSQL() {
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-EXO-DEV",
                "Cuaderno de notas",
                null,
                null, null, null, null, null
        ));
        var exhibicion = crearExhibicion("IT Exhibicion devolucion", EstadoExhibicion.ACTIVA);
        var relacion = exhibicionObjetoService.crear(
                request(exhibicion.id(), objeto.id(), EstadoExhibicionObjeto.EN_EXHIBICION)
        );

        var response = exhibicionObjetoService.verificarDevolucion(relacion.id(), null, "Devuelto sin observaciones");

        assertThat(response.estado()).isEqualTo(EstadoExhibicionObjeto.DEVUELTO);
        assertThat(response.devolucionVerificada()).isTrue();
        assertThat(response.fechaVerificacion()).isNotNull();
        assertThat(exhibicionObjetoRepository.findById(relacion.id()))
                .get()
                .satisfies(entity -> {
                    assertThat(entity.getEstado()).isEqualTo(EstadoExhibicionObjeto.DEVUELTO);
                    assertThat(entity.getDevolucionVerificada()).isTrue();
                    assertThat(entity.getObservacionesDevolucion()).isEqualTo("Devuelto sin observaciones");
                });
    }

    private com.proveedores.dto.ExhibicionResponseDTO crearExhibicion(String nombre, EstadoExhibicion estado) {
        return exhibicionService.crear(new ExhibicionRequestDTO(
                nombre,
                "Exhibicion generada por test de integracion",
                TipoExhibicion.TEMPORAL,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                estado
        ));
    }

    private ExhibicionObjetoRequestDTO request(Long exhibicionId, Long objetoId, EstadoExhibicionObjeto estado) {
        return new ExhibicionObjetoRequestDTO(
                exhibicionId,
                objetoId,
                LocalDate.now(),
                null,
                estado,
                false,
                null,
                null,
                null
        );
    }
}
