package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.exception.BusinessException;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.service.ObjetoMuseoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ObjetoMuseoServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjetoMuseoService objetoMuseoService;

    @Autowired
    private ObjetoMuseoRepository objetoMuseoRepository;

    @Test
    void creaObjetoRealEnBasePostgreSQL() {
        var response = objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-OBJ-001",
                "Brujula de campania",
                "Instrumento",
                "Alta generada por test de integracion"
        ));

        assertThat(response.id()).isNotNull();
        assertThat(objetoMuseoRepository.findById(response.id()))
                .get()
                .satisfies(objeto -> {
                    assertThat(objeto.getNumeroInventario()).isEqualTo("IT-OBJ-001");
                    assertThat(objeto.getNombre()).isEqualTo("Brujula de campania");
                    assertThat(objeto.getEliminado()).isFalse();
                });
    }

    @Test
    void rechazaNumeroDeInventarioDuplicadoContraDatosPersistidos() {
        objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-OBJ-DUP",
                "Objeto original",
                "Documento",
                null
        ));

        assertThatThrownBy(() -> objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                "IT-OBJ-DUP",
                "Objeto duplicado",
                "Documento",
                null
        ))).isInstanceOf(BusinessException.class)
                .hasMessage("Ya existe un objeto con ese numero de inventario");
    }
}
