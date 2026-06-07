package com.proveedores.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.proveedores.dto.ConfigAlertasVencimientoDTO;
import com.proveedores.security.KeycloakJwtAuthenticationConverter;
import com.proveedores.security.SecurityConfig;
import com.proveedores.service.ComodatoPrestamoService;
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

@WebMvcTest(ComodatoPrestamoAdminController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class ComodatoPrestamoAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComodatoPrestamoService comodatoPrestamoService;

    @MockBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void adminPuedeListarComodatosYPrestamos() throws Exception {
        when(comodatoPrestamoService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/comodatos-prestamos").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void operatorNoPuedeListarComodatosYPrestamos() throws Exception {
        mockMvc.perform(get("/api/admin/comodatos-prestamos").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerNoPuedeListarComodatosYPrestamos() throws Exception {
        mockMvc.perform(get("/api/admin/comodatos-prestamos").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeActualizarFechaVencimiento() throws Exception {
        mockMvc.perform(patch("/api/admin/comodatos-prestamos/1/fecha-vencimiento")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaVencimiento\":\"2026-06-20\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void operatorNoPuedeActualizarFechaVencimiento() throws Exception {
        mockMvc.perform(patch("/api/admin/comodatos-prestamos/1/fecha-vencimiento")
                        .with(user("operator").roles("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaVencimiento\":\"2026-06-20\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeActualizarConfigAlertas() throws Exception {
        when(comodatoPrestamoService.actualizarConfigAlertas(new ConfigAlertasVencimientoDTO(21)))
                .thenReturn(new ConfigAlertasVencimientoDTO(21));

        mockMvc.perform(put("/api/admin/comodatos-prestamos/config-alertas")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diasAnticipacion\":21}"))
                .andExpect(status().isOk());
    }
}
