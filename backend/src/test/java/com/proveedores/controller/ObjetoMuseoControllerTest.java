package com.proveedores.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.proveedores.service.ObjetoMuseoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Test
    void crearObjetoMuseoCorrectamenteDevuelveCreated() throws Exception {
        ObjetoMuseoResponseDTO response = new ObjetoMuseoResponseDTO(1L, "INV-1", "Casco", "Equipo", null);
        when(objetoMuseoService.crear(any(ObjetoMuseoRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/objetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ObjetoMuseoRequestDTO("INV-1", "Casco", "Equipo", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.numeroInventario").value("INV-1"));
    }

    @Test
    void requestInvalidoDevuelveBadRequest() throws Exception {
        mockMvc.perform(post("/api/objetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ObjetoMuseoRequestDTO("", "", "Equipo", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error de validacion"))
                .andExpect(jsonPath("$.errors.numeroInventario").exists())
                .andExpect(jsonPath("$.errors.nombre").exists());
    }

    @Test
    void resourceNotFoundDevuelve404() throws Exception {
        when(objetoMuseoService.obtenerPorId(99L)).thenThrow(new ResourceNotFoundException("Objeto de museo no encontrado"));

        mockMvc.perform(get("/api/objetos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Objeto de museo no encontrado"));
    }

    @Test
    void businessExceptionDevuelve400() throws Exception {
        when(objetoMuseoService.crear(any(ObjetoMuseoRequestDTO.class))).thenThrow(new BusinessException("Ya existe un objeto con ese numero de inventario"));

        mockMvc.perform(post("/api/objetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ObjetoMuseoRequestDTO("INV-1", "Casco", "Equipo", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ya existe un objeto con ese numero de inventario"));
    }
}
