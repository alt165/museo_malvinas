package com.proveedores.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.proveedores.dto.ColeccionObjetoResponseDTO;
import com.proveedores.security.KeycloakJwtAuthenticationConverter;
import com.proveedores.security.SecurityConfig;
import com.proveedores.service.ColeccionObjetoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ColeccionObjetoController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class ColeccionObjetoSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ColeccionObjetoService coleccionObjetoService;

    @MockBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void viewerPuedeConsultarColeccionesYObjetosAsociados() throws Exception {
        when(coleccionObjetoService.listar()).thenReturn(List.of());
        when(coleccionObjetoService.listarObjetos(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/colecciones").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/colecciones/1/objetos").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isOk());
    }

    @Test
    void viewerNoPuedeModificarColecciones() throws Exception {
        String body = "{\"nombre\":\"Coleccion\",\"descripcion\":\"Descripcion\"}";

        mockMvc.perform(post("/api/colecciones").contentType(MediaType.APPLICATION_JSON).content(body).with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/colecciones/1").contentType(MediaType.APPLICATION_JSON).content(body).with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/colecciones/1").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerNoPuedeAsociarNiQuitarObjetos() throws Exception {
        String body = "{\"objetoIds\":[1]}";

        mockMvc.perform(post("/api/colecciones/1/objetos").contentType(MediaType.APPLICATION_JSON).content(body).with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/colecciones/1/objetos/1").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminYOperatorPuedenCrearColecciones() throws Exception {
        String body = "{\"nombre\":\"Coleccion\",\"descripcion\":\"Descripcion\"}";
        when(coleccionObjetoService.crear(any())).thenReturn(new ColeccionObjetoResponseDTO(1L, "Coleccion", "Descripcion", true, 0L));

        mockMvc.perform(post("/api/colecciones").contentType(MediaType.APPLICATION_JSON).content(body).with(user("admin").roles("ADMIN")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/colecciones").contentType(MediaType.APPLICATION_JSON).content(body).with(user("operator").roles("OPERATOR")))
                .andExpect(status().isCreated());
    }
}
