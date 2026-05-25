package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.dto.CategoriaObjetoRequestDTO;
import com.proveedores.dto.CargaRapidaObjetoRequestDTO;
import com.proveedores.dto.MoverObjetoRequestDTO;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.entity.Depositante;
import com.proveedores.entity.EstadoConservacion;
import com.proveedores.entity.EstadoInventario;
import com.proveedores.entity.Inventario;
import com.proveedores.entity.OrigenCargaObjeto;
import com.proveedores.entity.TipoDepositante;
import com.proveedores.entity.Ubicacion;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.DepositanteRepository;
import com.proveedores.repository.InventarioRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.repository.UbicacionRepository;
import com.proveedores.service.CategoriaObjetoService;
import com.proveedores.service.ObjetoMuseoService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class ObjetoMuseoServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjetoMuseoService objetoMuseoService;

    @Autowired
    private CategoriaObjetoService categoriaObjetoService;

    @Autowired
    private ObjetoMuseoRepository objetoMuseoRepository;

    @Autowired
    private DepositanteRepository depositanteRepository;

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private UbicacionRepository ubicacionRepository;

    @Test
    void creaObjetoRealEnBasePostgreSQL() {
        var response = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-OBJ-001",
                "Brujula de campania",
                "Alta generada por test de integracion",
                null, null, null, null, null
        ));

        assertThat(response.id()).isNotNull();
        assertThat(objetoMuseoRepository.findById(response.id()))
                .get()
                .satisfies(objeto -> {
                    assertThat(objeto.getNumeroInventario()).isEqualTo("IT-OBJ-001");
                    assertThat(objeto.getDenominacionObjeto()).isEqualTo("Brujula de campania");
                    assertThat(objeto.getEliminado()).isFalse();
                });
    }

    @Test
    void rechazaNumeroDeInventarioDuplicadoContraDatosPersistidos() {
        objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-OBJ-DUP",
                "Objeto original",
                null,
                null, null, null, null, null
        ));

        assertThatThrownBy(() -> objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-OBJ-DUP",
                "Objeto duplicado",
                null,
                null, null, null, null, null
        ))).isInstanceOf(BusinessException.class)
                .hasMessage("Ya existe un objeto con ese numero de inventario");
    }

    @Test
    void buscaObjetosPorNombreEnBaseDeDatos() {
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-BUS-NOM-001",
                "Alfa Busqueda Patrimonial",
                null,
                null, null, null, null, null
        ));

        var resultado = objetoMuseoService.buscar("alfa busqueda", null, null, PageRequest.of(0, 20));

        assertThat(resultado.getContent()).extracting("id").contains(objeto.id());
    }

    @Test
    void buscaObjetosPorNumeroInventarioParcial() {
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-BUS-INV-XYZ-001",
                "Objeto por inventario",
                null,
                null, null, null, null, null
        ));

        var resultado = objetoMuseoService.buscar(null, "INV-XYZ", null, PageRequest.of(0, 20));

        assertThat(resultado.getContent()).extracting("id").contains(objeto.id());
    }

    @Test
    void buscaObjetosPorCategoriaSinDuplicados() {
        var categoria = categoriaObjetoService.crear(new CategoriaObjetoRequestDTO("IT Categoria busqueda", null));
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-BUS-CAT-001",
                "Objeto con categoria buscable",
                null,
                null, null, null, null, Set.of(categoria.id())
        ));
        objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-BUS-CAT-002",
                "Objeto sin categoria buscable",
                null,
                null, null, null, null, null
        ));

        var resultado = objetoMuseoService.buscar(null, null, java.util.List.of(categoria.id()), PageRequest.of(0, 20));

        assertThat(resultado.getContent()).extracting("id").containsExactly(objeto.id());
    }

    @Test
    void buscaObjetosConFiltrosCombinados() {
        var categoria = categoriaObjetoService.crear(new CategoriaObjetoRequestDTO("IT Categoria combinada", null));
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-BUS-COM-001",
                "Objeto combinado correcto",
                null,
                null, null, null, null, Set.of(categoria.id())
        ));
        objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-BUS-COM-002",
                "Objeto combinado fuera categoria",
                null,
                null, null, null, null, null
        ));

        var resultado = objetoMuseoService.buscar(
                "combinado",
                "COM-001",
                java.util.List.of(categoria.id()),
                PageRequest.of(0, 20)
        );

        assertThat(resultado.getContent()).extracting("id").containsExactly(objeto.id());
    }

    @Test
    void buscaObjetosConPaginacion() {
        objetoMuseoService.crear(new ObjetoMuseoRequestDTO("IT-BUS-PAG-001", "Objeto pagina comun", null, null, null, null, null, null));
        objetoMuseoService.crear(new ObjetoMuseoRequestDTO("IT-BUS-PAG-002", "Objeto pagina comun", null, null, null, null, null, null));
        objetoMuseoService.crear(new ObjetoMuseoRequestDTO("IT-BUS-PAG-003", "Objeto pagina comun", null, null, null, null, null, null));

        var resultado = objetoMuseoService.buscar(
                "Objeto pagina comun",
                null,
                null,
                PageRequest.of(0, 2, Sort.by("numeroInventario"))
        );

        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getTotalPages()).isEqualTo(2);
    }

    @Test
    void listadoDevuelveFechaIngresoDesdeInventario() {
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-FEC-ING-001",
                "Objeto con fecha de ingreso",
                null,
                null, null, null, EstadoConservacion.BUENO, null
        ));
        crearInventario(objeto.id(), LocalDate.of(2024, 4, 15));

        var resultado = objetoMuseoService.buscar("Objeto con fecha de ingreso", null, null, PageRequest.of(0, 20));

        assertThat(resultado.getContent())
                .filteredOn(item -> item.id().equals(objeto.id()))
                .singleElement()
                .satisfies(item -> assertThat(item.fechaIngreso()).isEqualTo(LocalDate.of(2024, 4, 15)));
    }

    @Test
    void ordenaPorNumeroInventarioYDenominacion() {
        objetoMuseoService.crear(new ObjetoMuseoRequestDTO("IT-SORT-NUM-002", "B objeto sort", null, null, null, null, null, null));
        objetoMuseoService.crear(new ObjetoMuseoRequestDTO("IT-SORT-NUM-001", "A objeto sort", null, null, null, null, null, null));

        var porNumero = objetoMuseoService.buscar(
                "objeto sort",
                null,
                null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "numeroInventario"))
        );
        var porDenominacion = objetoMuseoService.buscar(
                "objeto sort",
                null,
                null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "denominacionObjeto"))
        );

        assertThat(porNumero.getContent()).extracting("numeroInventario")
                .containsSubsequence("IT-SORT-NUM-001", "IT-SORT-NUM-002");
        assertThat(porDenominacion.getContent()).extracting("denominacionObjeto")
                .containsSubsequence("B objeto sort", "A objeto sort");
    }

    @Test
    void ordenaPorFechaIngresoConJoinInventario() {
        var objetoReciente = objetoMuseoService.crear(new ObjetoMuseoRequestDTO("IT-SORT-FEC-002", "Objeto fecha sort", null, null, null, null, null, null));
        var objetoAntiguo = objetoMuseoService.crear(new ObjetoMuseoRequestDTO("IT-SORT-FEC-001", "Objeto fecha sort", null, null, null, null, null, null));
        crearInventario(objetoReciente.id(), LocalDate.of(2024, 6, 1));
        crearInventario(objetoAntiguo.id(), LocalDate.of(2024, 1, 1));

        var resultado = objetoMuseoService.buscar(
                "Objeto fecha sort",
                null,
                null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "fechaIngreso"))
        );

        assertThat(resultado.getContent()).extracting("id")
                .containsSubsequence(objetoAntiguo.id(), objetoReciente.id());
        assertThat(resultado.getContent().get(0).fechaIngreso()).isEqualTo(LocalDate.of(2024, 1, 1));
    }

    @Test
    void busquedaPaginacionYSortFuncionanJuntos() {
        objetoMuseoService.crear(new ObjetoMuseoRequestDTO("IT-COMBO-SORT-003", "Objeto combo sort", "ccc", null, null, null, null, null));
        objetoMuseoService.crear(new ObjetoMuseoRequestDTO("IT-COMBO-SORT-001", "Objeto combo sort", "aaa", null, null, null, null, null));
        objetoMuseoService.crear(new ObjetoMuseoRequestDTO("IT-COMBO-SORT-002", "Objeto combo sort", "bbb", null, null, null, null, null));

        var resultado = objetoMuseoService.buscar(
                "Objeto combo sort",
                "COMBO",
                null,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "descripcion"))
        );

        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getContent()).extracting("descripcion").containsExactly("aaa", "bbb");
    }

    @Test
    void objetoEliminadoNoApareceEnConsultaNormalYSeListaConAuditoria() {
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-DEL-001",
                "Objeto eliminado auditable",
                "Descripcion eliminada",
                null, null, null, null, null
        ));

        objetoMuseoService.bajaLogica(objeto.id(), "admin-test");

        var consultaNormal = objetoMuseoService.buscar("Objeto eliminado auditable", null, null, PageRequest.of(0, 20));
        var eliminados = objetoMuseoService.listarEliminados(PageRequest.of(0, 20));

        assertThat(consultaNormal.getContent()).extracting("id").doesNotContain(objeto.id());
        assertThat(eliminados.getContent())
                .filteredOn(item -> item.id().equals(objeto.id()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.eliminadoPor()).isEqualTo("admin-test");
                    assertThat(item.fechaEliminacion()).isNotNull();
                });
    }

    @Test
    void restauraObjetoEliminadoYVuelveAConsultaNormal() {
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-DEL-REST",
                "Objeto restaurable",
                null,
                null, null, null, null, null
        ));

        objetoMuseoService.bajaLogica(objeto.id(), "admin-test");
        var restaurado = objetoMuseoService.restaurar(objeto.id(), "admin-test");
        var consultaNormal = objetoMuseoService.buscar("Objeto restaurable", null, null, PageRequest.of(0, 20));

        assertThat(restaurado.id()).isEqualTo(objeto.id());
        assertThat(consultaNormal.getContent()).extracting("id").contains(objeto.id());
        assertThat(objetoMuseoRepository.findById(objeto.id())).get().satisfies(entity -> {
            assertThat(entity.getEliminado()).isFalse();
            assertThat(entity.getActivo()).isTrue();
            assertThat(entity.getFechaEliminacion()).isNull();
            assertThat(entity.getEliminadoPor()).isNull();
        });
    }

    @Test
    void altaRapidaCreaObjetoPendiente() {
        Depositante depositante = crearDepositante("IT Depositante pendiente");

        var response = objetoMuseoService.cargaRapida(new CargaRapidaObjetoRequestDTO(
                depositante.getId(),
                "Objeto pendiente",
                "IT-PEND-001",
                "Descripcion breve pendiente"
        ), "operador-test");

        assertThat(objetoMuseoRepository.findById(response.objeto().id())).get().satisfies(objeto -> {
            assertThat(objeto.getOrigenCarga()).isEqualTo(OrigenCargaObjeto.RAPIDA);
            assertThat(objeto.getDatosCompletos()).isFalse();
            assertThat(objeto.getFechaCargaRapida()).isNotNull();
            assertThat(objeto.getCargaRapidaPor()).isEqualTo("operador-test");
        });
    }

    @Test
    void fichaCompletaMarcaObjetoRapidoComoDatosCompletos() {
        Depositante depositante = crearDepositante("IT Depositante completo");
        var categoria = categoriaObjetoService.crear(new CategoriaObjetoRequestDTO("IT Categoria completa", null));
        var response = objetoMuseoService.cargaRapida(new CargaRapidaObjetoRequestDTO(
                depositante.getId(),
                "Objeto a completar",
                "IT-PEND-002",
                "Descripcion breve completar"
        ), "operador-test");

        objetoMuseoService.actualizar(response.objeto().id(), new ObjetoMuseoRequestDTO(
                "IT-PEND-002",
                "Objeto a completar",
                "Descripcion breve completar",
                "Descripcion tecnica completa",
                "Metal y tela",
                "10 x 20 cm",
                EstadoConservacion.BUENO,
                Set.of(categoria.id())
        ));

        assertThat(objetoMuseoRepository.findById(response.objeto().id())).get()
                .satisfies(objeto -> assertThat(objeto.getDatosCompletos()).isTrue());
    }

    @Test
    void endpointListaSoloPendientesYNoEliminados() {
        Depositante depositante = crearDepositante("IT Depositante listado");
        var pendiente = objetoMuseoService.cargaRapida(new CargaRapidaObjetoRequestDTO(
                depositante.getId(),
                "Objeto pendiente visible",
                "IT-PEND-003",
                "Descripcion breve visible"
        ), "operador-test");
        var eliminado = objetoMuseoService.cargaRapida(new CargaRapidaObjetoRequestDTO(
                depositante.getId(),
                "Objeto pendiente eliminado",
                "IT-PEND-004",
                "Descripcion breve eliminado"
        ), "operador-test");
        objetoMuseoService.bajaLogica(eliminado.objeto().id(), "admin-test");

        var resultado = objetoMuseoService.listarPendientesCompletar(PageRequest.of(0, 20));

        assertThat(resultado.getContent()).extracting("id").contains(pendiente.objeto().id());
        assertThat(resultado.getContent()).extracting("id").doesNotContain(eliminado.objeto().id());
        assertThat(resultado.getContent())
                .filteredOn(item -> item.id().equals(pendiente.objeto().id()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.depositanteNombre()).isEqualTo(depositante.getNombre());
                    assertThat(item.reciboId()).isNotNull();
                    assertThat(item.reciboPdfUrl()).contains("/api/recibos/");
                });
    }

    @Test
    void altaRapidaAsignaUbicacionPreIngreso() {
        Depositante depositante = crearDepositante("IT Depositante pre ingreso");

        var response = objetoMuseoService.cargaRapida(new CargaRapidaObjetoRequestDTO(
                depositante.getId(),
                "Objeto pre ingreso",
                "IT-MOV-PRE-001",
                "Descripcion breve pre ingreso"
        ), "operador-test");

        assertThat(inventarioRepository.findByObjetoMuseoIdAndEliminadoFalse(response.objeto().id()))
                .get()
                .satisfies(inventario -> assertThat(inventario.getUbicacion().getNombre()).isEqualTo("Pre ingreso"));
    }

    @Test
    void moverObjetoActualizaUbicacionYCreaMovimiento() {
        var origen = crearUbicacion("IT Movimiento origen");
        var destino = crearUbicacion("IT Movimiento destino");
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-MOV-001",
                "Objeto movible",
                null,
                null, null, null, EstadoConservacion.BUENO, null,
                origen.getId()
        ));

        var movimiento = objetoMuseoService.mover(objeto.id(), new MoverObjetoRequestDTO(destino.getId(), "Traslado de prueba"), "operador-test");

        assertThat(inventarioRepository.findByObjetoMuseoIdAndEliminadoFalse(objeto.id()))
                .get()
                .satisfies(inventario -> assertThat(inventario.getUbicacion().getId()).isEqualTo(destino.getId()));
        assertThat(movimiento.fechaMovimiento()).isNotNull();
        assertThat(movimiento.ubicacionOrigen()).isEqualTo(origen.getNombre());
        assertThat(movimiento.ubicacionDestino()).isEqualTo(destino.getNombre());
        assertThat(movimiento.descripcion()).isEqualTo("Traslado de prueba");
        assertThat(movimiento.usuarioMovimiento()).isEqualTo("operador-test");
    }

    @Test
    void movimientosPorObjetoVienenOrdenadosDesc() {
        var origen = crearUbicacion("IT Movimiento desc origen");
        var destino1 = crearUbicacion("IT Movimiento desc destino 1");
        var destino2 = crearUbicacion("IT Movimiento desc destino 2");
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-MOV-DESC-001",
                "Objeto historial desc",
                null,
                null, null, null, EstadoConservacion.BUENO, null,
                origen.getId()
        ));

        objetoMuseoService.mover(objeto.id(), new MoverObjetoRequestDTO(destino1.getId(), "Primer movimiento"), "operador-test");
        objetoMuseoService.mover(objeto.id(), new MoverObjetoRequestDTO(destino2.getId(), "Segundo movimiento"), "operador-test");

        assertThat(objetoMuseoService.listarMovimientos(objeto.id()))
                .extracting("descripcion")
                .containsSubsequence("Segundo movimiento", "Primer movimiento", "Alta completa");
    }

    @Test
    void noPermiteMoverObjetoEliminadoOUbicacionInactiva() {
        var origen = crearUbicacion("IT Movimiento bloqueado origen");
        var inactiva = crearUbicacion("IT Movimiento bloqueado inactiva");
        inactiva.setActivo(false);
        inactiva.setEliminado(true);
        ubicacionRepository.save(inactiva);
        var objeto = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-MOV-BLOCK-001",
                "Objeto bloqueado",
                null,
                null, null, null, EstadoConservacion.BUENO, null,
                origen.getId()
        ));

        assertThatThrownBy(() -> objetoMuseoService.mover(objeto.id(), new MoverObjetoRequestDTO(inactiva.getId(), "No debe mover"), "operador-test"))
                .isInstanceOf(ResourceNotFoundException.class);

        objetoMuseoService.bajaLogica(objeto.id(), "admin-test");
        assertThatThrownBy(() -> objetoMuseoService.mover(objeto.id(), new MoverObjetoRequestDTO(origen.getId(), "No debe mover"), "operador-test"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void crearInventario(Long objetoId, LocalDate fechaIngreso) {
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setNombre("IT Ubicacion " + objetoId + " " + fechaIngreso);
        Ubicacion savedUbicacion = ubicacionRepository.save(ubicacion);

        Inventario inventario = new Inventario();
        inventario.setObjetoMuseo(objetoMuseoRepository.findById(objetoId).orElseThrow());
        inventario.setUbicacion(savedUbicacion);
        inventario.setEstado(EstadoInventario.DISPONIBLE);
        inventario.setEstadoConservacion(EstadoConservacion.BUENO);
        inventario.setFechaIngreso(fechaIngreso);
        inventario.setFechaUltimoMovimiento(LocalDateTime.now());
        inventarioRepository.save(inventario);
    }

    private Depositante crearDepositante(String nombre) {
        Depositante depositante = new Depositante();
        depositante.setNombre(nombre);
        depositante.setTipo(TipoDepositante.PERSONA);
        return depositanteRepository.save(depositante);
    }

    private Ubicacion crearUbicacion(String nombre) {
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setNombre(nombre);
        return ubicacionRepository.save(ubicacion);
    }
}
