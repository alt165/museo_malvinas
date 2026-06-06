package com.proveedores.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.GlobalExceptionHandler;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.security.KeycloakJwtAuthenticationConverter;
import com.proveedores.service.FotoObjetoMuseoService;
import com.proveedores.service.ObjetoMuseoService;
import com.proveedores.service.ReciboEscaneadoObjetoMuseoService;
import com.proveedores.service.ReciboIngresoObjetoService;
import com.proveedores.service.RelacionObjetoService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ObjetoMuseoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ObjetoMuseoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ObjetoMuseoService objetoMuseoService;

    @MockBean
    private FotoObjetoMuseoService fotoObjetoMuseoService;

    @MockBean
    private ReciboEscaneadoObjetoMuseoService reciboEscaneadoObjetoMuseoService;

    @MockBean
    private ReciboIngresoObjetoService reciboIngresoObjetoService;

    @MockBean
    private RelacionObjetoService relacionObjetoService;

    @MockBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Test
    void crearObjetoMuseoCorrectamenteDevuelveCreated() throws Exception {
        ObjetoMuseoResponseDTO response = new ObjetoMuseoResponseDTO(1L, "INV-1", "Casco", null, null, null, null, null, null, null, null, null, null, java.util.List.of(), java.util.List.of(), null);
        when(objetoMuseoService.crear(any(ObjetoMuseoRequestDTO.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/objetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ObjetoMuseoRequestDTO("INV-1", "Casco", null, null, null, null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.numeroInventario").value("INV-1"));
    }

    @Test
    void buscarObjetosPaginadosDevuelvePage() throws Exception {
        ObjetoMuseoResponseDTO response = new ObjetoMuseoResponseDTO(1L, "INV-1", "Casco", null, null, null, null, null, null, null, null, null, null, java.util.List.of(), java.util.List.of(), null);
        when(objetoMuseoService.buscar(eq("Casco"), eq("INV"), eq(java.util.List.of(2L)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(response)));

        mockMvc.perform(get("/api/objetos/buscar")
                        .param("nombre", "Casco")
                        .param("numeroInventario", "INV")
                        .param("categoriaIds", "2")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].numeroInventario").value("INV-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void requestInvalidoDevuelveBadRequest() throws Exception {
        mockMvc.perform(post("/api/objetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ObjetoMuseoRequestDTO("", "", null, null, null, null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("La solicitud contiene errores de validacion"))
                .andExpect(jsonPath("$.path").value("/api/objetos"))
                .andExpect(jsonPath("$.validationErrors.numeroInventario").value("El numero de inventario es obligatorio"))
                .andExpect(jsonPath("$.validationErrors.denominacionONombreValida").value("La denominacion o nombre es obligatorio"));
    }

    @Test
    void resourceNotFoundDevuelve404() throws Exception {
        when(objetoMuseoService.obtenerPorId(99L)).thenThrow(new ResourceNotFoundException("Objeto de museo no encontrado"));

        mockMvc.perform(get("/api/objetos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Objeto de museo no encontrado"))
                .andExpect(jsonPath("$.path").value("/api/objetos/99"));
    }

    @Test
    void businessExceptionDevuelve400() throws Exception {
        when(objetoMuseoService.crear(any(ObjetoMuseoRequestDTO.class), any())).thenThrow(new BusinessException("Ya existe un objeto con ese numero de inventario"));

        mockMvc.perform(post("/api/objetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ObjetoMuseoRequestDTO("INV-1", "Casco", null, null, null, null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Ya existe un objeto con ese numero de inventario"));
    }

    @Test
    void dataIntegrityViolationDevuelve409() throws Exception {
        when(objetoMuseoService.crear(any(ObjetoMuseoRequestDTO.class), any()))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        mockMvc.perform(post("/api/objetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ObjetoMuseoRequestDTO("INV-1", "Casco", null, null, null, null, null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("No se pudo completar la operacion porque viola una restriccion de datos"))
                .andExpect(jsonPath("$.path").value("/api/objetos"));
    }

    @Test
    void jsonInvalidoDevuelve400ConFormatoEstandar() throws Exception {
        mockMvc.perform(post("/api/objetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El cuerpo de la solicitud es invalido o no tiene el formato esperado"))
                .andExpect(jsonPath("$.path").value("/api/objetos"));
    }

    @Test
    void exceptionGenericaDevuelve500() throws Exception {
        when(objetoMuseoService.obtenerPorId(1L)).thenThrow(new RuntimeException("fallo inesperado"));

        mockMvc.perform(get("/api/objetos/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Error interno del servidor"))
                .andExpect(jsonPath("$.path").value("/api/objetos/1"));
    }
}
