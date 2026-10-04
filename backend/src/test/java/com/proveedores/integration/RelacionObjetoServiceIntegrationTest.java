package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.dto.CategoriaObjetoRequestDTO;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.EstadoConservacion;
import com.proveedores.entity.EmbargoObjeto;
import com.proveedores.entity.Fuerza;
import com.proveedores.entity.ObjetoVeterano;
import com.proveedores.entity.Veterano;
import com.proveedores.entity.VisibilidadCampo;
import com.proveedores.dto.RelacionObjetoRequestDTO;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.RelacionObjetoRepository;
import com.proveedores.repository.EmbargoObjetoRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.repository.ObjetoVeteranoRepository;
import com.proveedores.repository.VeteranoRepository;
import com.proveedores.service.CategoriaObjetoService;
import com.proveedores.service.ObjetoMuseoService;
import com.proveedores.service.RelacionObjetoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Set;
import java.util.HashMap;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class RelacionObjetoServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private RelacionObjetoService relacionObjetoService;

    @Autowired
    private ObjetoMuseoService objetoMuseoService;

    @Autowired
    private RelacionObjetoRepository relacionObjetoRepository;

    @Autowired
    private ObjetoMuseoRepository objetoMuseoRepository;

    @Autowired
    private ObjetoVeteranoRepository objetoVeteranoRepository;

    @Autowired
    private VeteranoRepository veteranoRepository;

    @Autowired
    private CategoriaObjetoService categoriaObjetoService;

    @Autowired
    private EmbargoObjetoRepository embargoObjetoRepository;

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

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
        assertThat(response.objetoOrigenNumeroInventario()).isEqualTo(origen.numeroInventario());
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

        assertThat(relaciones).extracting("idRelacion").contains("OBJETO_RELACION-" + saliente.id(), "OBJETO_RELACION-" + entrante.id());
        assertThat(relaciones).filteredOn(item -> item.idRelacion().equals("OBJETO_RELACION-" + saliente.id()))
                .singleElement()
                .satisfies(item -> assertThat(item.direccion()).isEqualTo("SALIENTE"));
        assertThat(relaciones).filteredOn(item -> item.idRelacion().equals("OBJETO_RELACION-" + entrante.id()))
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

        assertThat(grafo.nodes()).extracting("id").containsExactlyInAnyOrder("OBJETO-" + inicial.id(), "OBJETO-" + directo.id());
        assertThat(grafo.edges()).hasSize(1);
        assertThat(grafo.edges()).extracting("source").contains("OBJETO-" + inicial.id());
    }

    @Test
    void grafoProfundidadDosExpandeSegundoNivel() {
        var inicial = crearObjeto("IT-GRAFO-P2-001", "Objeto grafo inicial");
        var directo = crearObjeto("IT-GRAFO-P2-002", "Objeto grafo directo");
        var segundoNivel = crearObjeto("IT-GRAFO-P2-003", "Objeto grafo segundo nivel");
        relacionObjetoService.crear(new RelacionObjetoRequestDTO(inicial.id(), directo.id(), "aparece en", null));
        relacionObjetoService.crear(new RelacionObjetoRequestDTO(directo.id(), segundoNivel.id(), "vinculado a", null));

        var grafo = relacionObjetoService.obtenerGrafoRelaciones(inicial.id(), 2);

        assertThat(grafo.nodes()).extracting("id").contains("OBJETO-" + inicial.id(), "OBJETO-" + directo.id(), "OBJETO-" + segundoNivel.id());
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

        assertThat(grafo.nodes()).extracting("id").containsExactlyInAnyOrder("OBJETO-" + uno.id(), "OBJETO-" + dos.id(), "OBJETO-" + tres.id());
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

        assertThat(grafo.nodes()).extracting("id").containsExactly("OBJETO-" + inicial.id());
        assertThat(grafo.edges()).isEmpty();
    }

    @Test
    void grafoObjetoInexistenteDevuelveResourceNotFound() {
        assertThatThrownBy(() -> relacionObjetoService.obtenerGrafoRelaciones(999999L, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Objeto de museo no encontrado");
    }

    @Test
    void integraPersonaConVariosObjetosEnTablaYGrafoSinColisiones() {
        var uniforme = crearObjeto("IT-PER-001", "Uniforme");
        var fotografia = crearObjeto("IT-PER-002", "Fotografia");
        Veterano persona = crearPersona("Juan", "Perez");
        vincular(uniforme.id(), persona, "pertenecio a");
        vincular(fotografia.id(), persona, "retrata a");

        var tablaPersona = relacionObjetoService.listarPorPersona(persona.getId());
        var tablaObjeto = relacionObjetoService.listarPorObjeto(uniforme.id());
        var grafo = relacionObjetoService.obtenerGrafoRelacionesPersona(persona.getId(), 1);

        assertThat(tablaPersona).extracting("elementoId").containsExactlyInAnyOrder(uniforme.id(), fotografia.id());
        assertThat(tablaObjeto).singleElement().satisfies(item -> {
            assertThat(item.tipoElemento().name()).isEqualTo("PERSONA");
            assertThat(item.elementoId()).isEqualTo(persona.getId());
        });
        assertThat(grafo.nodes()).extracting("id").containsExactlyInAnyOrder(
                "PERSONA-" + persona.getId(), "OBJETO-" + uniforme.id(), "OBJETO-" + fotografia.id());
        assertThat(grafo.nodes()).extracting("id").doesNotHaveDuplicates();
        assertThat(grafo.edges()).hasSize(2);
    }

    @Test
    void grafoObjetoCombinaRelacionesConPersonaYOtroObjeto() {
        var uniforme = crearObjeto("IT-MIX-001", "Uniforme");
        var documento = crearObjeto("IT-MIX-002", "Documento");
        var fotografia = crearObjeto("IT-MIX-003", "Fotografia");
        Veterano persona = crearPersona("Ana", "Gomez");
        relacionObjetoService.crear(new RelacionObjetoRequestDTO(uniforme.id(), documento.id(), "documentado por", null));
        vincular(uniforme.id(), persona, "pertenecio a");
        vincular(fotografia.id(), persona, "retrata a");

        var grafo = relacionObjetoService.obtenerGrafoRelaciones(uniforme.id(), 1);

        assertThat(grafo.nodes()).extracting("id").contains(
                "OBJETO-" + uniforme.id(), "OBJETO-" + documento.id(),
                "PERSONA-" + persona.getId(), "OBJETO-" + fotografia.id());
        assertThat(grafo.edges()).extracting("tipoVinculo").extracting(Object::toString)
                .contains("OBJETO_OBJETO", "OBJETO_PERSONA");
    }

    @Test
    void personaYObjetoSinRelacionesDevuelvenResultadosVacios() {
        var objeto = crearObjeto("IT-EMPTY-001", "Objeto aislado");
        Veterano persona = crearPersona("Sin", "Objetos");

        assertThat(relacionObjetoService.listarPorObjeto(objeto.id())).isEmpty();
        assertThat(relacionObjetoService.listarPorPersona(persona.getId())).isEmpty();
        assertThat(relacionObjetoService.obtenerGrafoRelacionesPersona(persona.getId(), 1).nodes())
                .extracting("id").containsExactly("PERSONA-" + persona.getId());
    }

    @Test
    void viewerNoRecibeObjetoEmbargadoNiIndirectamenteDesdePersona() {
        var visible = crearObjeto("IT-SEC-001", "Objeto visible");
        var embargado = crearObjeto("IT-SEC-002", "Objeto embargado");
        Veterano persona = crearPersona("Persona", "Visible");
        vincular(visible.id(), persona, "vinculado a");
        vincular(embargado.id(), persona, "vinculado a");
        EmbargoObjeto embargo = new EmbargoObjeto();
        embargo.setObjetoMuseo(objetoMuseoRepository.findById(embargado.id()).orElseThrow());
        embargo.setFechaInicio(LocalDate.now());
        embargoObjetoRepository.save(embargo);
        var objetoVisible = objetoMuseoRepository.findById(visible.id()).orElseThrow();
        var visibilidades = new HashMap<String, VisibilidadCampo>();
        visibilidades.put("denominacionObjeto", VisibilidadCampo.PRIVADO);
        visibilidades.put("numeroInventario", VisibilidadCampo.PRIVADO);
        objetoVisible.setVisibilidades(visibilidades);
        objetoMuseoRepository.save(objetoVisible);
        autenticarComo("ROLE_VIEWER");

        var tabla = relacionObjetoService.listarPorPersona(persona.getId());
        var grafo = relacionObjetoService.obtenerGrafoRelacionesPersona(persona.getId(), 2);

        assertThat(tabla).extracting("elementoId").containsExactly(visible.id());
        assertThat(grafo.nodes()).extracting("id")
                .contains("PERSONA-" + persona.getId(), "OBJETO-" + visible.id())
                .doesNotContain("OBJETO-" + embargado.id());
        assertThat(grafo.edges()).noneMatch(edge -> edge.target().equals("OBJETO-" + embargado.id()));
        assertThat(grafo.nodes()).filteredOn(node -> node.id().equals("OBJETO-" + visible.id()))
                .singleElement()
                .satisfies(node -> {
                    assertThat(node.label()).isEqualTo("Objeto patrimonial");
                    assertThat(node.numeroInventario()).isNull();
                });
    }

    private com.proveedores.dto.ObjetoMuseoResponseDTO crearObjeto(String numeroInventario, String denominacion) {
        var categoria = categoriaObjetoService.crear(new CategoriaObjetoRequestDTO("Categoria " + numeroInventario, null));
        return objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                numeroInventario,
                denominacion,
                "Descripcion " + denominacion,
                "Descripcion tecnica " + denominacion,
                "Material de prueba",
                "10 cm",
                EstadoConservacion.BUENO,
                Set.of(categoria.id()),
                null,
                1L,
                CaracterRecepcionObjeto.DONACION,
                null
        ));
    }

    private Veterano crearPersona(String nombre, String apellido) {
        Veterano persona = new Veterano();
        persona.setNombre(nombre);
        persona.setApellido(apellido);
        persona.setFuerza(Fuerza.EJERCITO);
        return veteranoRepository.save(persona);
    }

    private void vincular(Long objetoId, Veterano persona, String tipoRelacion) {
        ObjetoVeterano relacion = new ObjetoVeterano();
        relacion.setObjetoMuseo(objetoMuseoRepository.findById(objetoId).orElseThrow());
        relacion.setVeterano(persona);
        relacion.setTipoRelacion(tipoRelacion);
        objetoVeteranoRepository.save(relacion);
    }

    private void autenticarComo(String authority) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "tester", "n/a", Set.of(new SimpleGrantedAuthority(authority))));
    }
}
