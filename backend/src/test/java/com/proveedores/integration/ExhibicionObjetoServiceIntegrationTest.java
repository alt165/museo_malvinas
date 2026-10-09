package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.dto.ExhibicionObjetoRequestDTO;
import com.proveedores.dto.ExhibicionRequestDTO;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.EstadoExhibicionObjeto;
import com.proveedores.entity.TipoExhibicion;
import com.proveedores.exception.ConflictException;
import com.proveedores.repository.ExhibicionObjetoRepository;
import com.proveedores.repository.UsuarioRepository;
import com.proveedores.service.ExhibicionObjetoService;
import com.proveedores.service.ExhibicionService;
import com.proveedores.service.ObjetoMuseoService;
import com.proveedores.testfixture.ObjetoMuseoTestFixture;
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

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void impideObjetoEnMasDeUnaExhibicionActiva() {
        var objeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-EXO-001", "Cantimplora"));
        var exhibicionActiva = crearExhibicion("IT Exhibicion activa A", EstadoExhibicion.ACTIVA);
        var otraExhibicionActiva = crearExhibicion("IT Exhibicion activa B", EstadoExhibicion.ACTIVA);

        exhibicionObjetoService.crear(request(exhibicionActiva.id(), objeto.id()));

        assertThatThrownBy(() -> exhibicionObjetoService.crear(
                request(otraExhibicionActiva.id(), objeto.id())
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("rango de fechas coincidente");
    }

    @Test
    void endpointDeRelacionNoEvitaSolapamientoDeExhibicionesPlanificadas() {
        var objeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-EXO-PLAN", "Objeto planificado"));
        var primera = crearExhibicion("IT Planificada A", EstadoExhibicion.PLANIFICADA);
        var segunda = crearExhibicion("IT Planificada B", EstadoExhibicion.PLANIFICADA);

        exhibicionObjetoService.crear(request(primera.id(), objeto.id()));

        assertThatThrownBy(() -> exhibicionObjetoService.crear(request(segunda.id(), objeto.id())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void endpointDeRelacionDerivaEstadoYFechasDesdeLaExhibicion() {
        var objeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-EXO-NOFAKE", "Objeto sin devolución falsa"));
        var exhibicion = crearExhibicion("IT Sin devolución falsa", EstadoExhibicion.ACTIVA);

        var response = exhibicionObjetoService.crear(new ExhibicionObjetoRequestDTO(exhibicion.id(), objeto.id()));

        assertThat(response.estado()).isEqualTo(EstadoExhibicionObjeto.EN_EXHIBICION);
        assertThat(response.devolucionVerificada()).isFalse();
        assertThat(response.fechaRetiro()).isNull();
        assertThat(response.fechaVerificacion()).isNull();
        assertThat(response.verificadoPorUsuarioId()).isNull();
    }

    @Test
    void verificaDevolucionEnPostgreSQL() {
        var objeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-EXO-DEV", "Cuaderno de notas"));
        var exhibicion = crearExhibicion("IT Exhibicion devolucion", EstadoExhibicion.ACTIVA);
        var relacion = exhibicionObjetoService.crear(
                request(exhibicion.id(), objeto.id())
        );

        var usuario = new com.proveedores.entity.Usuario();
        usuario.setNombre("Museóloga de prueba");
        usuario.setEmail("museologa-devolucion@example.test");
        usuario.setKeycloakId("kc-devolucion-it");
        usuario.setFechaCreacion(com.proveedores.time.MuseoTime.now());
        usuarioRepository.save(usuario);

        var response = exhibicionObjetoService.verificarDevolucion(relacion.id(), "Devuelto sin observaciones", usuario.getEmail());

        assertThat(response.estado()).isEqualTo(EstadoExhibicionObjeto.DEVUELTO);
        assertThat(response.devolucionVerificada()).isTrue();
        assertThat(response.fechaVerificacion()).isNotNull();
        assertThat(exhibicionObjetoRepository.findById(relacion.id()))
                .get()
                .satisfies(entity -> {
                    assertThat(entity.getEstado()).isEqualTo(EstadoExhibicionObjeto.DEVUELTO);
                    assertThat(entity.getDevolucionVerificada()).isTrue();
                    assertThat(entity.getVerificadoPor().getId()).isEqualTo(usuario.getId());
                    assertThat(entity.getFechaVerificacion()).isNotNull();
                    assertThat(entity.getFechaRetiro()).isEqualTo(LocalDate.now());
                    assertThat(entity.getObservacionesDevolucion()).isEqualTo("Devuelto sin observaciones");
                });
    }

    @Test
    void recrearAsociacionEliminadaReactivaElMismoRegistro() {
        var objeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-EXO-REACT", "Objeto reactivable"));
        var exhibicion = crearExhibicion("IT Exhibicion reactivable", EstadoExhibicion.PLANIFICADA);
        var creada = exhibicionObjetoService.crear(request(exhibicion.id(), objeto.id()));
        exhibicionObjetoService.bajaLogica(creada.id(), null);

        var reactivada = exhibicionObjetoService.crear(request(exhibicion.id(), objeto.id()));

        assertThat(reactivada.id()).isEqualTo(creada.id());
        assertThat(exhibicionObjetoRepository.findById(creada.id())).get()
                .satisfies(relacion -> {
                    assertThat(relacion.getActivo()).isTrue();
                    assertThat(relacion.getEliminado()).isFalse();
                });
    }

    private com.proveedores.dto.ExhibicionResponseDTO crearExhibicion(String nombre, EstadoExhibicion estado) {
        LocalDate inicio = estado == EstadoExhibicion.PLANIFICADA ? LocalDate.now().plusDays(10) : LocalDate.now();
        return exhibicionService.crear(new ExhibicionRequestDTO(
                nombre,
                "Exhibicion generada por test de integracion",
                TipoExhibicion.TEMPORAL,
                inicio,
                inicio.plusDays(30),
                estado
        ));
    }

    private ExhibicionObjetoRequestDTO request(Long exhibicionId, Long objetoId) {
        return new ExhibicionObjetoRequestDTO(exhibicionId, objetoId);
    }
}
