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
import com.proveedores.repository.ExhibicionRepository;
import com.proveedores.repository.UsuarioRepository;
import com.proveedores.service.ExhibicionObjetoService;
import com.proveedores.service.ExhibicionService;
import com.proveedores.service.ObjetoMuseoService;
import com.proveedores.testfixture.ObjetoMuseoTestFixture;
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

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void editarPeriodoNoPuedeCrearSolapamiento() {
        var objeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-EXH-EDIT", "Objeto edición"));
        LocalDate inicio = LocalDate.now().plusDays(10);
        exhibicionService.crear(new ExhibicionRequestDTO(
                "IT Reserva original", null, TipoExhibicion.TEMPORAL,
                inicio, inicio.plusDays(10), EstadoExhibicion.PLANIFICADA, java.util.Set.of(objeto.id())
        ));
        var segunda = exhibicionService.crear(new ExhibicionRequestDTO(
                "IT Reserva editable", null, TipoExhibicion.TEMPORAL,
                inicio.plusDays(11), inicio.plusDays(20), EstadoExhibicion.PLANIFICADA, java.util.Set.of(objeto.id())
        ));

        assertThatThrownBy(() -> exhibicionService.actualizar(segunda.id(), new ExhibicionRequestDTO(
                segunda.nombre(), segunda.descripcion(), TipoExhibicion.TEMPORAL,
                inicio.plusDays(5), inicio.plusDays(15), EstadoExhibicion.PLANIFICADA, java.util.Set.of(objeto.id())
        ))).isInstanceOf(ConflictException.class);
    }

    @Test
    void exhibicionPermanenteBloqueaReservasPosteriores() {
        var objeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-EXH-PERM", "Objeto permanente"));
        LocalDate inicio = LocalDate.now().plusDays(10);
        exhibicionService.crear(new ExhibicionRequestDTO(
                "IT Permanente", null, TipoExhibicion.PERMANENTE,
                inicio, null, EstadoExhibicion.PLANIFICADA, java.util.Set.of(objeto.id())
        ));

        assertThatThrownBy(() -> exhibicionService.crear(new ExhibicionRequestDTO(
                "IT Posterior", null, TipoExhibicion.TEMPORAL,
                inicio.plusDays(30), inicio.plusDays(40), EstadoExhibicion.PLANIFICADA, java.util.Set.of(objeto.id())
        ))).isInstanceOf(ConflictException.class);
    }

    @Test
    void cancelarAntesDelInicioLiberaReservaSinCrearDevolucionFisica() {
        var objeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-EXH-CANCEL", "Objeto cancelado"));
        LocalDate inicio = LocalDate.now().plusDays(10);
        var exhibicion = exhibicionService.crear(new ExhibicionRequestDTO(
                "IT Cancelable", null, TipoExhibicion.TEMPORAL,
                inicio, inicio.plusDays(10), EstadoExhibicion.PLANIFICADA, java.util.Set.of(objeto.id())
        ));
        Long relacionId = exhibicion.objetos().get(0).id();

        exhibicionService.cancelar(exhibicion.id());

        assertThat(exhibicionObjetoRepository.findById(relacionId)).get().satisfies(relacion -> {
            assertThat(relacion.getEliminado()).isTrue();
            assertThat(relacion.getDevolucionVerificada()).isFalse();
            assertThat(relacion.getFechaRetiro()).isNull();
            assertThat(relacion.getFechaVerificacion()).isNull();
            assertThat(relacion.getVerificadoPor()).isNull();
        });
        assertThat(exhibicionService.buscarObjetosDisponibilidad(
                objeto.numeroInventario(), inicio, inicio.plusDays(10), null,
                org.springframework.data.domain.PageRequest.of(0, 10)
        ).getContent()).singleElement().satisfies(item -> assertThat(item.disponible()).isTrue());
    }

    @Test
    void finalizaExhibicionConObjetosPendientesSinInventarVerificacion() {
        var objeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-EXH-PEND", "Mapa de sala"));
        var exhibicion = crearExhibicion("IT Exhibicion pendiente");

        exhibicionObjetoService.crear(new ExhibicionObjetoRequestDTO(
                exhibicion.id(),
                objeto.id()
        ));

        var response = exhibicionService.finalizar(exhibicion.id());

        assertThat(response.estado()).isEqualTo(EstadoExhibicion.FINALIZADA);
        assertThat(exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(exhibicion.id()))
                .singleElement()
                .satisfies(relacion -> {
                    assertThat(relacion.getEstado()).isEqualTo(EstadoExhibicionObjeto.PENDIENTE_REVISION);
                    assertThat(relacion.getDevolucionVerificada()).isFalse();
                    assertThat(relacion.getFechaRetiro()).isNull();
                    assertThat(relacion.getFechaVerificacion()).isNull();
                    assertThat(relacion.getVerificadoPor()).isNull();
                });
    }

    @Test
    void finalizaExhibicionCuandoTodosLosObjetosFueronDevueltos() {
        var objeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-EXH-FIN", "Panel fotografico"));
        var exhibicion = crearExhibicion("IT Exhibicion finalizable");
        var relacion = exhibicionObjetoService.crear(new ExhibicionObjetoRequestDTO(
                exhibicion.id(),
                objeto.id()
        ));

        var usuario = new com.proveedores.entity.Usuario();
        usuario.setNombre("Verificador integración");
        usuario.setEmail("verificador-exhibicion@example.test");
        usuario.setKeycloakId("kc-verificador-exhibicion");
        usuario.setFechaCreacion(com.proveedores.time.MuseoTime.now());
        usuarioRepository.save(usuario);
        exhibicionObjetoService.verificarDevolucion(relacion.id(), "Devuelto", usuario.getEmail());

        var response = exhibicionService.finalizar(exhibicion.id());

        assertThat(response.estado()).isEqualTo(EstadoExhibicion.FINALIZADA);
        assertThat(exhibicionRepository.findById(exhibicion.id()))
                .get()
                .satisfies(entity -> assertThat(entity.getEstado()).isEqualTo(EstadoExhibicion.FINALIZADA));
    }

    @Test
    void objetoSoloQuedaDisponibleDespuesDeVerificarLaDevolucion() {
        var objeto = objetoMuseoService.crear(ObjetoMuseoTestFixture.valido("IT-EXH-REUSE", "Pelota historica"));
        var exhibicion = crearExhibicion("IT Exhibicion finalizada hoy");

        exhibicionObjetoService.crear(new ExhibicionObjetoRequestDTO(
                exhibicion.id(),
                objeto.id()
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
                .contains(false);

        var relacion = exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(exhibicion.id()).get(0);
        var usuario = new com.proveedores.entity.Usuario();
        usuario.setNombre("Verificador reutilización");
        usuario.setEmail("verificador-reutilizacion@example.test");
        usuario.setKeycloakId("kc-verificador-reutilizacion");
        usuario.setFechaCreacion(com.proveedores.time.MuseoTime.now());
        usuarioRepository.save(usuario);
        exhibicionObjetoService.verificarDevolucion(relacion.getId(), "Retorno físico confirmado", usuario.getEmail());

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
                .satisfies(asignacion -> assertThat(asignacion.objetoMuseoId()).isEqualTo(objeto.id()));
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
