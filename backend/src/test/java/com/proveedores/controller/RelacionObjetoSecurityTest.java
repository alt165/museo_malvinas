package com.proveedores.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.proveedores.dto.RelacionObjetoResponseDTO;
import com.proveedores.security.KeycloakJwtAuthenticationConverter;
import com.proveedores.security.SecurityConfig;
import com.proveedores.service.RelacionObjetoService;
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

@WebMvcTest(RelacionObjetoController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class RelacionObjetoSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RelacionObjetoService relacionObjetoService;

    @MockBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void viewerPuedeConsultarRelaciones() throws Exception {
        when(relacionObjetoService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/relaciones-objetos").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isOk());
    }

    @Test
    void viewerNoPuedeCrearEditarNiEliminarRelaciones() throws Exception {
        String body = "{\"objetoOrigenId\":1,\"objetoDestinoId\":2,\"tipoRelacion\":\"similar\",\"descripcion\":null}";

        mockMvc.perform(post("/api/relaciones-objetos").contentType(MediaType.APPLICATION_JSON).content(body).with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/relaciones-objetos/1").contentType(MediaType.APPLICATION_JSON).content(body).with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/relaciones-objetos/1").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminYOperatorPuedenCrearRelaciones() throws Exception {
        String body = "{\"objetoOrigenId\":1,\"objetoDestinoId\":2,\"tipoRelacion\":\"similar\",\"descripcion\":null}";
        when(relacionObjetoService.crear(any(), any())).thenReturn(new RelacionObjetoResponseDTO(
                1L,
                1L,
                "INV-1",
                "Origen",
                2L,
                "INV-2",
                "Destino",
                "similar",
                null,
                null,
                null
        ));

        mockMvc.perform(post("/api/relaciones-objetos").contentType(MediaType.APPLICATION_JSON).content(body).with(user("admin").roles("ADMIN")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/relaciones-objetos").contentType(MediaType.APPLICATION_JSON).content(body).with(user("operator").roles("OPERATOR")))
                .andExpect(status().isCreated());
    }
}
