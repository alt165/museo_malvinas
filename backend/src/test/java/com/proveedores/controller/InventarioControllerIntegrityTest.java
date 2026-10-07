package com.proveedores.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.proveedores.exception.ConflictException;
import com.proveedores.security.KeycloakJwtAuthenticationConverter;
import com.proveedores.security.SecurityConfig;
import com.proveedores.service.InventarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventarioController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class InventarioControllerIntegrityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventarioService inventarioService;

    @MockBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void putQueIntentaCambiarObjetoDevuelve409() throws Exception {
        when(inventarioService.actualizar(eq(1L), any())).thenThrow(new ConflictException("El objeto asociado al inventario es inmutable"));

        mockMvc.perform(put("/api/inventarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isConflict());
    }

    @Test
    void viewerNoPuedeModificarInventario() throws Exception {
        mockMvc.perform(put("/api/inventarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body())
                        .with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
    }

    private String body() {
        return "{\"objetoMuseoId\":99,\"ubicacionId\":1,\"estado\":\"DISPONIBLE\",\"estadoConservacion\":\"BUENO\",\"fechaIngreso\":\"2026-10-06\"}";
    }
}
