package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.dto.CategoriaObjetoRequestDTO;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.exception.BusinessException;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.service.CategoriaObjetoService;
import com.proveedores.service.ObjetoMuseoService;
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
}
