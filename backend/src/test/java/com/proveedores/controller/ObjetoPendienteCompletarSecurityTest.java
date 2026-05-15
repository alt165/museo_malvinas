package com.proveedores.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.proveedores.security.KeycloakJwtAuthenticationConverter;
import com.proveedores.security.SecurityConfig;
import com.proveedores.service.FotoObjetoMuseoService;
import com.proveedores.service.ObjetoMuseoService;
import com.proveedores.service.ReciboEscaneadoObjetoMuseoService;
import com.proveedores.service.ReciboIngresoObjetoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ObjetoMuseoController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class ObjetoPendienteCompletarSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ObjetoMuseoService objetoMuseoService;

    @MockBean
    private FotoObjetoMuseoService fotoObjetoMuseoService;

    @MockBean
    private ReciboEscaneadoObjetoMuseoService reciboEscaneadoObjetoMuseoService;

    @MockBean
    private ReciboIngresoObjetoService reciboIngresoObjetoService;

    @MockBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void adminPuedeListarPendientes() throws Exception {
        when(objetoMuseoService.listarPendientesCompletar(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/objetos/pendientes-completar").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void operatorPuedeListarPendientes() throws Exception {
        when(objetoMuseoService.listarPendientesCompletar(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/objetos/pendientes-completar").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk());
    }

    @Test
    void viewerNoPuedeListarPendientes() throws Exception {
        mockMvc.perform(get("/api/objetos/pendientes-completar").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
    }
}
