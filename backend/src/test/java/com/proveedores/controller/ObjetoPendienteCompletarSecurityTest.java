package com.proveedores.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.proveedores.security.KeycloakJwtAuthenticationConverter;
import com.proveedores.security.SecurityConfig;
import com.proveedores.service.ComodatoPrestamoService;
import com.proveedores.service.FotoObjetoMuseoService;
import com.proveedores.service.ObjetoMuseoExportService;
import com.proveedores.service.ObjetoMuseoService;
import com.proveedores.service.ReciboEscaneadoObjetoMuseoService;
import com.proveedores.service.ReciboIngresoObjetoService;
import com.proveedores.service.RelacionObjetoService;
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
    private ObjetoMuseoExportService objetoMuseoExportService;

    @MockBean
    private ComodatoPrestamoService comodatoPrestamoService;

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

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void viewerPuedeExportarPdfObjetos() throws Exception {
        when(objetoMuseoExportService.exportarListadoPdf(any(), any(), any(), any(), any())).thenReturn(new byte[] { 1, 2, 3 });

        mockMvc.perform(get("/api/objetos/export/pdf").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isOk());
    }

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
    void adminYOperatorPuedenExportarPendientes() throws Exception {
        when(objetoMuseoExportService.exportarPendientesCompletarPdf(any(), any())).thenReturn(new byte[] { 1, 2, 3 });

        mockMvc.perform(get("/api/objetos/pendientes-completar/export/pdf").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/objetos/pendientes-completar/export/pdf").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk());
    }

    @Test
    void viewerNoPuedeListarPendientes() throws Exception {
        mockMvc.perform(get("/api/objetos/pendientes-completar").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerNoPuedeExportarPendientes() throws Exception {
        mockMvc.perform(get("/api/objetos/pendientes-completar/export/pdf").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeListarVencimientosProximos() throws Exception {
        when(comodatoPrestamoService.listarVencimientosProximos(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/objetos/vencimientos-proximos").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void operatorNoPuedeListarVencimientosProximos() throws Exception {
        mockMvc.perform(get("/api/objetos/vencimientos-proximos").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerNoPuedeListarVencimientosProximos() throws Exception {
        mockMvc.perform(get("/api/objetos/vencimientos-proximos").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeListarMovimientosDeObjeto() throws Exception {
        when(objetoMuseoService.listarMovimientos(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/objetos/1/movimientos").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void operatorPuedeListarMovimientosDeObjeto() throws Exception {
        when(objetoMuseoService.listarMovimientos(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/objetos/1/movimientos").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk());
    }

    @Test
    void viewerNoPuedeListarMovimientosDeObjeto() throws Exception {
        mockMvc.perform(get("/api/objetos/1/movimientos").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerPuedeListarRelacionesDeObjeto() throws Exception {
        when(relacionObjetoService.listarPorObjeto(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/objetos/1/relaciones").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isOk());
    }

    @Test
    void viewerPuedeConsultarGrafoRelacionesDeObjeto() throws Exception {
        when(relacionObjetoService.obtenerGrafoRelaciones(1L, 1)).thenReturn(new com.proveedores.dto.ObjetoGrafoResponseDTO(List.of(), List.of()));

        mockMvc.perform(get("/api/objetos/1/grafo-relaciones").param("profundidad", "1").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isOk());
    }

    @Test
    void viewerNoPuedeListarUbicaciones() throws Exception {
        mockMvc.perform(get("/api/ubicaciones").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
    }
}
