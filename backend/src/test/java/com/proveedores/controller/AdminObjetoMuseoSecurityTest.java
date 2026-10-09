package com.proveedores.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.proveedores.security.KeycloakJwtAuthenticationConverter;
import com.proveedores.service.AuditoriaObjetoService;
import com.proveedores.service.ObjetoMuseoService;
import com.proveedores.service.FotoObjetoMuseoService;
import com.proveedores.dto.RegeneracionFotosPublicasResponseDTO;
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
import com.proveedores.security.SecurityConfig;

@WebMvcTest(AdminObjetoMuseoController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class AdminObjetoMuseoSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ObjetoMuseoService objetoMuseoService;

    @MockBean
    private FotoObjetoMuseoService fotoObjetoMuseoService;

    @MockBean
    private AuditoriaObjetoService auditoriaObjetoService;

    @MockBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void adminPuedeListarEliminados() throws Exception {
        when(objetoMuseoService.listarEliminados(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/objetos/eliminados").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void museologoNoPuedeListarEliminados() throws Exception {
        mockMvc.perform(get("/api/admin/objetos/eliminados").with(user("museologo").roles("MUSEOLOGO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeConsultarHistorial() throws Exception {
        when(auditoriaObjetoService.listarHistorial(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/objetos/1/historial").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void museologoNoPuedeConsultarHistorial() throws Exception {
        mockMvc.perform(get("/api/admin/objetos/1/historial").with(user("museologo").roles("MUSEOLOGO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerNoPuedeRestaurar() throws Exception {
        mockMvc.perform(post("/api/admin/objetos/1/restaurar").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloAdminPuedeRegenerarFotosPublicas() throws Exception {
        when(fotoObjetoMuseoService.regenerarPublicasFaltantes())
                .thenReturn(new RegeneracionFotosPublicasResponseDTO(0, 0, 0, 0));

        mockMvc.perform(post("/api/admin/objetos/fotos/regenerar-publicas").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/objetos/fotos/regenerar-publicas").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
    }
}
