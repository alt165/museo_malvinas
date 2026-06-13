package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.dto.AgregarObjetosColeccionRequestDTO;
import com.proveedores.dto.ColeccionObjetoRequestDTO;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.exception.BusinessException;
import com.proveedores.repository.ColeccionObjetoRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.service.AuditoriaObjetoService;
import com.proveedores.service.ColeccionObjetoService;
import com.proveedores.service.ObjetoMuseoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ColeccionObjetoServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ColeccionObjetoService coleccionObjetoService;

    @Autowired
    private ObjetoMuseoService objetoMuseoService;

    @Autowired
    private AuditoriaObjetoService auditoriaObjetoService;

    @Autowired
    private ColeccionObjetoRepository coleccionObjetoRepository;

    @Autowired
    private ObjetoMuseoRepository objetoMuseoRepository;

    @Test
    void creaYListaColeccion() {
        var coleccion = coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion principal", "Descripcion", null));

        assertThat(coleccion.id()).isNotNull();
        assertThat(coleccion.activo()).isTrue();
        assertThat(coleccion.cantidadObjetos()).isZero();
        assertThat(coleccionObjetoService.listar()).extracting("id").contains(coleccion.id());
    }

    @Test
    void rechazaNombreDuplicado() {
        coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion duplicada", null, null));

        assertThatThrownBy(() -> coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("it coleccion duplicada", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ya existe una coleccion con ese nombre");
    }

    @Test
    void agregaObjetoSinColeccionYLoQuita() {
        var coleccion = coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion objetos", null, null));
        var objeto = crearObjeto("IT-COL-001", "Objeto para coleccion");

        var objetos = coleccionObjetoService.agregarObjetos(coleccion.id(), new AgregarObjetosColeccionRequestDTO(List.of(objeto.id())), "operador-test");

        assertThat(objetos).extracting("id").containsExactly(objeto.id());
        assertThat(objetoMuseoRepository.findById(objeto.id()))
                .get()
                .extracting(item -> item.getColeccionObjeto().getId())
                .isEqualTo(coleccion.id());
        assertThat(auditoriaObjetoService.listarHistorial(objeto.id()))
                .anySatisfy(evento -> {
                    assertThat(evento.accion()).isEqualTo("INCORPORACION_COLECCION");
                    assertThat(evento.origen()).isEqualTo("COLECCION");
                    assertThat(evento.descripcion()).isEqualTo("El objeto fue incorporado a la colección: " + coleccion.nombre() + ".");
                    assertThat(evento.usuario()).isEqualTo("operador-test");
                    assertThat(evento.valoresNuevos()).contains("coleccionId", coleccion.nombre());
                });

        coleccionObjetoService.quitarObjeto(coleccion.id(), objeto.id(), "operador-test");

        assertThat(objetoMuseoRepository.findById(objeto.id()))
                .get()
                .extracting(item -> item.getColeccionObjeto())
                .isNull();
        assertThat(auditoriaObjetoService.listarHistorial(objeto.id()))
                .anySatisfy(evento -> {
                    assertThat(evento.accion()).isEqualTo("DESVINCULACION_COLECCION");
                    assertThat(evento.origen()).isEqualTo("COLECCION");
                    assertThat(evento.descripcion()).isEqualTo("El objeto fue desvinculado de la colección: " + coleccion.nombre() + ".");
                    assertThat(evento.usuario()).isEqualTo("operador-test");
                    assertThat(evento.valoresAnteriores()).contains("coleccionId", coleccion.nombre());
                });
    }

    @Test
    void rechazaObjetoYaAsociadoAOtraColeccion() {
        var primera = coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion primera", null, null));
        var segunda = coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion segunda", null, null));
        var objeto = crearObjeto("IT-COL-DUP-001", "Objeto ya asociado");

        coleccionObjetoService.agregarObjetos(primera.id(), new AgregarObjetosColeccionRequestDTO(List.of(objeto.id())), "operador-test");

        assertThatThrownBy(() -> coleccionObjetoService.agregarObjetos(segunda.id(), new AgregarObjetosColeccionRequestDTO(List.of(objeto.id())), "operador-test"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya pertenece a una coleccion");
    }

    @Test
    void listaObjetosSinColeccion() {
        var coleccion = coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion filtro", null, null));
        var sinColeccion = crearObjeto("IT-SIN-COL-001", "Objeto libre");
        var conColeccion = crearObjeto("IT-CON-COL-001", "Objeto asociado");
        coleccionObjetoService.agregarObjetos(coleccion.id(), new AgregarObjetosColeccionRequestDTO(List.of(conColeccion.id())), "operador-test");

        var resultado = objetoMuseoService.listarSinColeccion();

        assertThat(resultado).extracting("id").contains(sinColeccion.id());
        assertThat(resultado).extracting("id").doesNotContain(conColeccion.id());
    }

    @Test
    void bajaColeccionNoEliminaObjetosYLosDesvincula() {
        var coleccion = coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion baja", null, null));
        var objeto = crearObjeto("IT-COL-BAJA-001", "Objeto conservado");
        coleccionObjetoService.agregarObjetos(coleccion.id(), new AgregarObjetosColeccionRequestDTO(List.of(objeto.id())), "operador-test");

        coleccionObjetoService.bajaLogica(coleccion.id(), "operador-test");

        assertThat(coleccionObjetoRepository.findById(coleccion.id()))
                .get()
                .satisfies(entity -> {
                    assertThat(entity.getEliminado()).isTrue();
                    assertThat(entity.getActivo()).isFalse();
                    assertThat(entity.getFechaEliminacion()).isNotNull();
                });
        assertThat(objetoMuseoRepository.findById(objeto.id()))
                .get()
                .satisfies(entity -> {
                    assertThat(entity.getEliminado()).isFalse();
                    assertThat(entity.getColeccionObjeto()).isNull();
                });
        assertThat(auditoriaObjetoService.listarHistorial(objeto.id()))
                .anySatisfy(evento -> {
                    assertThat(evento.accion()).isEqualTo("DESVINCULACION_POR_ELIMINACION_COLECCION");
                    assertThat(evento.origen()).isEqualTo("ELIMINACION_COLECCION");
                    assertThat(evento.descripcion()).isEqualTo("El objeto quedó sin colección por eliminación de la colección: " + coleccion.nombre() + ".");
                    assertThat(evento.usuario()).isEqualTo("operador-test");
                    assertThat(evento.valoresAnteriores()).contains("coleccionId", coleccion.nombre());
                });
    }

    @Test
    void bajaColeccionGeneraHistorialParaCadaObjetoAsociado() {
        var coleccion = coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion baja multiple", null, null));
        var primerObjeto = crearObjeto("IT-COL-BAJA-MUL-001", "Objeto conservado uno");
        var segundoObjeto = crearObjeto("IT-COL-BAJA-MUL-002", "Objeto conservado dos");
        coleccionObjetoService.agregarObjetos(
                coleccion.id(),
                new AgregarObjetosColeccionRequestDTO(List.of(primerObjeto.id(), segundoObjeto.id())),
                "operador-multiple"
        );

        coleccionObjetoService.bajaLogica(coleccion.id(), "operador-multiple");

        assertThat(auditoriaObjetoService.listarHistorial(primerObjeto.id()))
                .anySatisfy(evento -> {
                    assertThat(evento.accion()).isEqualTo("DESVINCULACION_POR_ELIMINACION_COLECCION");
                    assertThat(evento.origen()).isEqualTo("ELIMINACION_COLECCION");
                    assertThat(evento.usuario()).isEqualTo("operador-multiple");
                });
        assertThat(auditoriaObjetoService.listarHistorial(segundoObjeto.id()))
                .anySatisfy(evento -> {
                    assertThat(evento.accion()).isEqualTo("DESVINCULACION_POR_ELIMINACION_COLECCION");
                    assertThat(evento.origen()).isEqualTo("ELIMINACION_COLECCION");
                    assertThat(evento.usuario()).isEqualTo("operador-multiple");
                });
        assertThat(objetoMuseoRepository.findById(primerObjeto.id())).get().extracting(item -> item.getColeccionObjeto()).isNull();
        assertThat(objetoMuseoRepository.findById(segundoObjeto.id())).get().extracting(item -> item.getColeccionObjeto()).isNull();
    }

    @Test
    void buscaDisponiblesParaColeccionExcluyeObjetosDeOtrasColecciones() {
        var coleccion = coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion busqueda otra", null, null));
        var libre = crearObjeto("IT-COL-BUS-LIBRE-001", "Objeto libre busqueda");
        var asociado = crearObjeto("IT-COL-BUS-ASOC-001", "Objeto asociado busqueda");
        coleccionObjetoService.agregarObjetos(coleccion.id(), new AgregarObjetosColeccionRequestDTO(List.of(asociado.id())), "operador-test");

        var resultado = objetoMuseoService.buscarDisponiblesParaColeccion("busqueda", null, List.of(), null, org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(resultado.getContent()).extracting("id").contains(libre.id());
        assertThat(resultado.getContent()).extracting("id").doesNotContain(asociado.id());
    }

    @Test
    void buscaDisponiblesParaEdicionIncluyeObjetosDeLaColeccionActual() {
        var coleccion = coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion busqueda edicion", null, null));
        var asociado = crearObjeto("IT-COL-BUS-EDIT-001", "Objeto busqueda edicion");
        coleccionObjetoService.agregarObjetos(coleccion.id(), new AgregarObjetosColeccionRequestDTO(List.of(asociado.id())), "operador-test");

        var resultado = objetoMuseoService.buscarDisponiblesParaColeccion("edicion", null, List.of(), coleccion.id(), org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(resultado.getContent()).extracting("id").contains(asociado.id());
    }

    @Test
    void actualizarColeccionConListaFinalAgregaYQuitaObjetos() {
        var coleccion = coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion sync", null, null));
        var removido = crearObjeto("IT-COL-SYNC-001", "Objeto removido sync");
        var agregado = crearObjeto("IT-COL-SYNC-002", "Objeto agregado sync");
        coleccionObjetoService.agregarObjetos(coleccion.id(), new AgregarObjetosColeccionRequestDTO(List.of(removido.id())), "operador-test");

        coleccionObjetoService.actualizar(coleccion.id(), new ColeccionObjetoRequestDTO("IT Coleccion sync", null, List.of(agregado.id())), "editor-test");

        assertThat(objetoMuseoRepository.findById(removido.id())).get().extracting(item -> item.getColeccionObjeto()).isNull();
        assertThat(objetoMuseoRepository.findById(agregado.id())).get().extracting(item -> item.getColeccionObjeto().getId()).isEqualTo(coleccion.id());
        assertThat(auditoriaObjetoService.listarHistorial(removido.id())).anySatisfy(evento -> assertThat(evento.accion()).isEqualTo("DESVINCULACION_COLECCION"));
        assertThat(auditoriaObjetoService.listarHistorial(agregado.id())).anySatisfy(evento -> assertThat(evento.accion()).isEqualTo("INCORPORACION_COLECCION"));
    }


    private com.proveedores.dto.ObjetoMuseoResponseDTO crearObjeto(String numeroInventario, String denominacion) {
        return objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                numeroInventario,
                denominacion,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1L,
                CaracterRecepcionObjeto.DONACION,
                null
        ));
    }
}
