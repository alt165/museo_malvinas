package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.dto.RelacionObjetoRequestDTO;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.RelacionObjetoRepository;
import com.proveedores.service.ObjetoMuseoService;
import com.proveedores.service.RelacionObjetoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RelacionObjetoServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private RelacionObjetoService relacionObjetoService;

    @Autowired
    private ObjetoMuseoService objetoMuseoService;

    @Autowired
    private RelacionObjetoRepository relacionObjetoRepository;

    @Test
    void creaRelacionValida() {
        var origen = crearObjeto("IT-REL-001", "Objeto origen relacion");
        var destino = crearObjeto("IT-REL-002", "Objeto destino relacion");

        var response = relacionObjetoService.crear(
                new RelacionObjetoRequestDTO(origen.id(), destino.id(), "acompanaba a", "Relacion documental"),
                "tester"
        );

        assertThat(response.id()).isNotNull();
        assertThat(response.objetoOrigenId()).isEqualTo(origen.id());
        assertThat(response.objetoOrigenNumeroInventario()).isEqualTo("IT-REL-001");
        assertThat(response.objetoDestinoId()).isEqualTo(destino.id());
        assertThat(response.fechaCreacion()).isNotNull();
        assertThat(response.creadoPor()).isEqualTo("tester");
    }

    @Test
    void rechazaRelacionConsigoMismo() {
        var objeto = crearObjeto("IT-REL-SELF", "Objeto autoconsulta");

        assertThatThrownBy(() -> relacionObjetoService.crear(new RelacionObjetoRequestDTO(objeto.id(), objeto.id(), "similar", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El objeto origen y destino no pueden ser el mismo");
    }

    @Test
    void rechazaObjetoInexistente() {
        var origen = crearObjeto("IT-REL-NF", "Objeto origen inexistente");

        assertThatThrownBy(() -> relacionObjetoService.crear(new RelacionObjetoRequestDTO(origen.id(), 999999L, "similar", null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Objeto de museo no encontrado");
    }

    @Test
    void rechazaObjetoEliminado() {
        var origen = crearObjeto("IT-REL-DEL-001", "Objeto origen activo");
        var destino = crearObjeto("IT-REL-DEL-002", "Objeto destino eliminado");
        objetoMuseoService.bajaLogica(destino.id(), "tester");

        assertThatThrownBy(() -> relacionObjetoService.crear(new RelacionObjetoRequestDTO(origen.id(), destino.id(), "similar", null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Objeto de museo no encontrado");
    }

    @Test
    void rechazaDuplicadoExactoActivo() {
        var origen = crearObjeto("IT-REL-DUP-001", "Objeto origen duplicado");
        var destino = crearObjeto("IT-REL-DUP-002", "Objeto destino duplicado");
        var request = new RelacionObjetoRequestDTO(origen.id(), destino.id(), "parte de", null);
        relacionObjetoService.crear(request);

        assertThatThrownBy(() -> relacionObjetoService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ya existe una relacion igual entre los objetos");
    }

    @Test
    void listaRelacionesDeObjetoComoOrigenYDestino() {
        var objeto = crearObjeto("IT-REL-LIST-001", "Objeto listado");
        var destino = crearObjeto("IT-REL-LIST-002", "Objeto destino listado");
        var origen = crearObjeto("IT-REL-LIST-003", "Objeto origen listado");
        var saliente = relacionObjetoService.crear(new RelacionObjetoRequestDTO(objeto.id(), destino.id(), "vinculado a", null));
        var entrante = relacionObjetoService.crear(new RelacionObjetoRequestDTO(origen.id(), objeto.id(), "referencia a", null));

        var relaciones = relacionObjetoService.listarPorObjeto(objeto.id());

        assertThat(relaciones).extracting("idRelacion").contains(saliente.id(), entrante.id());
        assertThat(relaciones).filteredOn(item -> item.idRelacion().equals(saliente.id()))
                .singleElement()
                .satisfies(item -> assertThat(item.direccion()).isEqualTo("SALIENTE"));
        assertThat(relaciones).filteredOn(item -> item.idRelacion().equals(entrante.id()))
                .singleElement()
                .satisfies(item -> assertThat(item.direccion()).isEqualTo("ENTRANTE"));
    }

    @Test
    void deleteHaceBajaLogica() {
        var origen = crearObjeto("IT-REL-BAJA-001", "Objeto origen baja");
        var destino = crearObjeto("IT-REL-BAJA-002", "Objeto destino baja");
        var relacion = relacionObjetoService.crear(new RelacionObjetoRequestDTO(origen.id(), destino.id(), "relacion baja", null));

        relacionObjetoService.bajaLogica(relacion.id());

        assertThat(relacionObjetoRepository.findById(relacion.id()))
                .get()
                .satisfies(entity -> {
                    assertThat(entity.getEliminado()).isTrue();
                    assertThat(entity.getActivo()).isFalse();
                    assertThat(entity.getFechaEliminacion()).isNotNull();
                });
    }

    @Test
    void grafoProfundidadUnoIncluyeObjetoInicialYRelacionesDirectas() {
        var inicial = crearObjeto("IT-GRAFO-P1-001", "Objeto grafo inicial");
        var directo = crearObjeto("IT-GRAFO-P1-002", "Objeto grafo directo");
        var segundoNivel = crearObjeto("IT-GRAFO-P1-003", "Objeto grafo segundo nivel");
        relacionObjetoService.crear(new RelacionObjetoRequestDTO(inicial.id(), directo.id(), "aparece en", null));
        relacionObjetoService.crear(new RelacionObjetoRequestDTO(directo.id(), segundoNivel.id(), "vinculado a", null));

        var grafo = relacionObjetoService.obtenerGrafoRelaciones(inicial.id(), 1);

        assertThat(grafo.nodes()).extracting("id").containsExactlyInAnyOrder(inicial.id(), directo.id());
        assertThat(grafo.edges()).hasSize(1);
        assertThat(grafo.edges()).extracting("source").contains(inicial.id());
    }

    @Test
    void grafoProfundidadDosExpandeSegundoNivel() {
        var inicial = crearObjeto("IT-GRAFO-P2-001", "Objeto grafo inicial");
        var directo = crearObjeto("IT-GRAFO-P2-002", "Objeto grafo directo");
        var segundoNivel = crearObjeto("IT-GRAFO-P2-003", "Objeto grafo segundo nivel");
        relacionObjetoService.crear(new RelacionObjetoRequestDTO(inicial.id(), directo.id(), "aparece en", null));
        relacionObjetoService.crear(new RelacionObjetoRequestDTO(directo.id(), segundoNivel.id(), "vinculado a", null));

        var grafo = relacionObjetoService.obtenerGrafoRelaciones(inicial.id(), 2);

        assertThat(grafo.nodes()).extracting("id").contains(inicial.id(), directo.id(), segundoNivel.id());
        assertThat(grafo.edges()).hasSize(2);
    }

    @Test
    void grafoEvitaCiclosYDuplicados() {
        var uno = crearObjeto("IT-GRAFO-CIC-001", "Objeto ciclo uno");
        var dos = crearObjeto("IT-GRAFO-CIC-002", "Objeto ciclo dos");
        var tres = crearObjeto("IT-GRAFO-CIC-003", "Objeto ciclo tres");
        relacionObjetoService.crear(new RelacionObjetoRequestDTO(uno.id(), dos.id(), "relacion", null));
        relacionObjetoService.crear(new RelacionObjetoRequestDTO(dos.id(), tres.id(), "relacion", null));
        relacionObjetoService.crear(new RelacionObjetoRequestDTO(tres.id(), uno.id(), "relacion", null));

        var grafo = relacionObjetoService.obtenerGrafoRelaciones(uno.id(), 3);

        assertThat(grafo.nodes()).extracting("id").containsExactlyInAnyOrder(uno.id(), dos.id(), tres.id());
        assertThat(grafo.edges()).extracting("id").doesNotHaveDuplicates();
        assertThat(grafo.nodes()).extracting("id").doesNotHaveDuplicates();
    }

    @Test
    void grafoRechazaProfundidadMayorATres() {
        var objeto = crearObjeto("IT-GRAFO-MAX-001", "Objeto grafo profundidad");

        assertThatThrownBy(() -> relacionObjetoService.obtenerGrafoRelaciones(objeto.id(), 4))
                .isInstanceOf(BusinessException.class)
                .hasMessage("La profundidad maxima permitida es 3");
    }

    @Test
    void grafoExcluyeRelacionesYObjetosEliminados() {
        var inicial = crearObjeto("IT-GRAFO-DEL-001", "Objeto grafo inicial");
        var destinoRelacionEliminada = crearObjeto("IT-GRAFO-DEL-002", "Objeto relacion eliminada");
        var destinoObjetoEliminado = crearObjeto("IT-GRAFO-DEL-003", "Objeto eliminado");
        var relacionEliminada = relacionObjetoService.crear(new RelacionObjetoRequestDTO(inicial.id(), destinoRelacionEliminada.id(), "eliminada", null));
        relacionObjetoService.crear(new RelacionObjetoRequestDTO(inicial.id(), destinoObjetoEliminado.id(), "objeto eliminado", null));
        relacionObjetoService.bajaLogica(relacionEliminada.id());
        objetoMuseoService.bajaLogica(destinoObjetoEliminado.id(), "tester");

        var grafo = relacionObjetoService.obtenerGrafoRelaciones(inicial.id(), 1);

        assertThat(grafo.nodes()).extracting("id").containsExactly(inicial.id());
        assertThat(grafo.edges()).isEmpty();
    }

    @Test
    void grafoObjetoInexistenteDevuelveResourceNotFound() {
        assertThatThrownBy(() -> relacionObjetoService.obtenerGrafoRelaciones(999999L, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Objeto de museo no encontrado");
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
