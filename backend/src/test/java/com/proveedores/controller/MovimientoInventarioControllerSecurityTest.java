package com.proveedores.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.proveedores.security.KeycloakJwtAuthenticationConverter;
import com.proveedores.security.SecurityConfig;
import com.proveedores.service.MovimientoInventarioService;
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

@WebMvcTest(MovimientoInventarioController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class MovimientoInventarioControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovimientoInventarioService movimientoInventarioService;

    @MockBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void losTresRolesPuedenConsultarElHistorial() throws Exception {
        when(movimientoInventarioService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/movimientos-inventario").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/movimientos-inventario").with(user("museologo").roles("MUSEOLOGO"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/movimientos-inventario").with(user("viewer").roles("VIEWER"))).andExpect(status().isOk());
    }

    @Test
    void unUsuarioAnonimoNoPuedeConsultarElHistorial() throws Exception {
        mockMvc.perform(get("/api/movimientos-inventario")).andExpect(status().isUnauthorized());
    }

    @Test
    void elHistorialNoExponeCreacionEdicionNiEliminacionCrud() throws Exception {
        String body = "{}";

        mockMvc.perform(post("/api/movimientos-inventario").contentType(MediaType.APPLICATION_JSON).content(body).with(user("admin").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/api/movimientos-inventario/1").contentType(MediaType.APPLICATION_JSON).content(body).with(user("admin").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/movimientos-inventario/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/api/movimientos-inventario/1").contentType(MediaType.APPLICATION_JSON).content(body).with(user("museologo").roles("MUSEOLOGO")))
                .andExpect(status().isMethodNotAllowed());
    }
}
