package com.proveedores.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.proveedores.dto.ExhibicionObjetoResponseDTO;
import com.proveedores.entity.EstadoExhibicionObjeto;
import com.proveedores.exception.ConflictException;
import com.proveedores.security.KeycloakJwtAuthenticationConverter;
import com.proveedores.security.SecurityConfig;
import com.proveedores.service.ExhibicionObjetoService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExhibicionObjetoController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class ExhibicionObjetoControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExhibicionObjetoService exhibicionObjetoService;

    @MockBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void adminYMuseologoUsanLaMismaAutoridadDeAsignacion() throws Exception {
        when(exhibicionObjetoService.crear(any(), anyString())).thenReturn(response());

        mockMvc.perform(post("/api/exhibiciones-objetos").contentType(MediaType.APPLICATION_JSON).content(body()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/exhibiciones-objetos").contentType(MediaType.APPLICATION_JSON).content(body()).with(user("museologo").roles("MUSEOLOGO")))
                .andExpect(status().isCreated());
    }

    @Test
    void viewerNoPuedeAsignarObjetos() throws Exception {
        mockMvc.perform(post("/api/exhibiciones-objetos").contentType(MediaType.APPLICATION_JSON).content(body()).with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void conflictoDeDominioEnEndpointDirectoDevuelve409() throws Exception {
        when(exhibicionObjetoService.crear(any(), nullable(String.class))).thenThrow(new ConflictException("Período incompatible"));

        mockMvc.perform(post("/api/exhibiciones-objetos").contentType(MediaType.APPLICATION_JSON).content(body()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isConflict());
    }

    @Test
    void noExistePutGenericoParaReescribirLaAsignacion() throws Exception {
        mockMvc.perform(put("/api/exhibiciones-objetos/1").contentType(MediaType.APPLICATION_JSON).content(body()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
    }

    private String body() {
        return "{\"exhibicionId\":1,\"objetoMuseoId\":2}";
    }

    private ExhibicionObjetoResponseDTO response() {
        return new ExhibicionObjetoResponseDTO(1L, 1L, "Exhibición", 2L, "INV-2", "Objeto", LocalDate.now(), null,
                EstadoExhibicionObjeto.EN_EXHIBICION, false, null, null, null, null);
    }
}
