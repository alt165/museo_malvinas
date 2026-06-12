package com.proveedores.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.proveedores.dto.DepositanteResponseDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.entity.TipoDepositante;
import com.proveedores.exception.GlobalExceptionHandler;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.security.KeycloakJwtAuthenticationConverter;
import com.proveedores.service.DepositanteExportService;
import com.proveedores.service.DepositanteService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DepositanteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DepositanteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DepositanteService depositanteService;

    @MockBean
    private DepositanteExportService depositanteExportService;

    @MockBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Test
    void buscarIdentificacionDevuelveDepositante() throws Exception {
        when(depositanteService.buscarPorIdentificacion("12.345.678"))
                .thenReturn(new DepositanteResponseDTO(1L, "Juan Perez", TipoDepositante.PERSONA, "juan@example.com", "12.345.678", null, null));

        mockMvc.perform(get("/api/depositantes/buscar-identificacion").param("valor", "12.345.678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.dni").value("12.345.678"));
    }

    @Test
    void buscarIdentificacionNoEncontradaDevuelve404() throws Exception {
        when(depositanteService.buscarPorIdentificacion("99.999.999"))
                .thenThrow(new ResourceNotFoundException("Depositante no encontrado"));

        mockMvc.perform(get("/api/depositantes/buscar-identificacion").param("valor", "99.999.999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Depositante no encontrado"));
    }

    @Test
    void buscarNombreDevuelveListaDeDepositantes() throws Exception {
        when(depositanteService.buscarPorNombre("juan"))
                .thenReturn(List.of(new DepositanteResponseDTO(1L, "Juan Perez", TipoDepositante.PERSONA, "juan@example.com", "12.345.678", null, null)));

        mockMvc.perform(get("/api/depositantes/buscar-nombre").param("valor", "juan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nombre").value("Juan Perez"))
                .andExpect(jsonPath("$[0].dni").value("12.345.678"));
    }

    @Test
    void listarObjetosDevuelveObjetosDelDepositante() throws Exception {
        ObjetoMuseoResponseDTO objeto = new ObjetoMuseoResponseDTO(1L, "INV-1", "Casco", null, null, null, null, null, null, null, null, null, null, List.of(), List.of(), null);
        when(depositanteService.listarObjetos(1L)).thenReturn(List.of(objeto));

        mockMvc.perform(get("/api/depositantes/1/objetos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].numeroInventario").value("INV-1"));
    }

    @Test
    void exportarObjetosPdfDevuelveArchivo() throws Exception {
        when(depositanteExportService.exportarObjetosPdf(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn("%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/depositantes/1/objetos/export/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PDF_VALUE)))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("depositante_1_objetos_")));
    }
}
