package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.dto.AgregarObjetosColeccionRequestDTO;
import com.proveedores.dto.ColeccionObjetoRequestDTO;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.exception.BusinessException;
import com.proveedores.repository.ColeccionObjetoRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
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
    private ColeccionObjetoRepository coleccionObjetoRepository;

    @Autowired
    private ObjetoMuseoRepository objetoMuseoRepository;

    @Test
    void creaYListaColeccion() {
        var coleccion = coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion principal", "Descripcion"));

        assertThat(coleccion.id()).isNotNull();
        assertThat(coleccion.activo()).isTrue();
        assertThat(coleccion.cantidadObjetos()).isZero();
        assertThat(coleccionObjetoService.listar()).extracting("id").contains(coleccion.id());
    }

    @Test
    void rechazaNombreDuplicado() {
        coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion duplicada", null));

        assertThatThrownBy(() -> coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("it coleccion duplicada", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ya existe una coleccion con ese nombre");
    }

    @Test
    void agregaObjetoSinColeccionYLoQuita() {
        var coleccion = coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion objetos", null));
        var objeto = crearObjeto("IT-COL-001", "Objeto para coleccion");

        var objetos = coleccionObjetoService.agregarObjetos(coleccion.id(), new AgregarObjetosColeccionRequestDTO(List.of(objeto.id())));

        assertThat(objetos).extracting("id").containsExactly(objeto.id());
        assertThat(objetoMuseoRepository.findById(objeto.id()))
                .get()
                .extracting(item -> item.getColeccionObjeto().getId())
                .isEqualTo(coleccion.id());

        coleccionObjetoService.quitarObjeto(coleccion.id(), objeto.id());

        assertThat(objetoMuseoRepository.findById(objeto.id()))
                .get()
                .extracting(item -> item.getColeccionObjeto())
                .isNull();
    }

    @Test
    void rechazaObjetoYaAsociadoAOtraColeccion() {
        var primera = coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion primera", null));
        var segunda = coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion segunda", null));
        var objeto = crearObjeto("IT-COL-DUP-001", "Objeto ya asociado");

        coleccionObjetoService.agregarObjetos(primera.id(), new AgregarObjetosColeccionRequestDTO(List.of(objeto.id())));

        assertThatThrownBy(() -> coleccionObjetoService.agregarObjetos(segunda.id(), new AgregarObjetosColeccionRequestDTO(List.of(objeto.id()))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya pertenece a una coleccion");
    }

    @Test
    void listaObjetosSinColeccion() {
        var coleccion = coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion filtro", null));
        var sinColeccion = crearObjeto("IT-SIN-COL-001", "Objeto libre");
        var conColeccion = crearObjeto("IT-CON-COL-001", "Objeto asociado");
        coleccionObjetoService.agregarObjetos(coleccion.id(), new AgregarObjetosColeccionRequestDTO(List.of(conColeccion.id())));

        var resultado = objetoMuseoService.listarSinColeccion();

        assertThat(resultado).extracting("id").contains(sinColeccion.id());
        assertThat(resultado).extracting("id").doesNotContain(conColeccion.id());
    }

    @Test
    void bajaColeccionNoEliminaObjetosYLosDesvincula() {
        var coleccion = coleccionObjetoService.crear(new ColeccionObjetoRequestDTO("IT Coleccion baja", null));
        var objeto = crearObjeto("IT-COL-BAJA-001", "Objeto conservado");
        coleccionObjetoService.agregarObjetos(coleccion.id(), new AgregarObjetosColeccionRequestDTO(List.of(objeto.id())));

        coleccionObjetoService.bajaLogica(coleccion.id());

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
                null
        ));
    }
}
