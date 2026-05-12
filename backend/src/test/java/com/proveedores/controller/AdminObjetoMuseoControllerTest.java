package com.proveedores.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.proveedores.dto.ObjetoMuseoEliminadoResponseDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.exception.GlobalExceptionHandler;
import com.proveedores.security.KeycloakJwtAuthenticationConverter;
import com.proveedores.service.ObjetoMuseoService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminObjetoMuseoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminObjetoMuseoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ObjetoMuseoService objetoMuseoService;

    @MockBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Test
    void listarEliminadosDevuelvePageConAuditoria() throws Exception {
        var eliminado = new ObjetoMuseoEliminadoResponseDTO(
                1L,
                "INV-1",
                "Casco",
                "Descripcion breve",
                LocalDateTime.of(2026, 5, 12, 10, 0),
                "admin",
                null,
                List.of()
        );
        when(objetoMuseoService.listarEliminados(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(eliminado)));

        mockMvc.perform(get("/api/admin/objetos/eliminados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].eliminadoPor").value("admin"))
                .andExpect(jsonPath("$.content[0].fechaEliminacion").exists());
    }

    @Test
    void restaurarPasaUsuarioActualAlServicio() throws Exception {
        when(objetoMuseoService.restaurar(eq(1L), eq("admin-id")))
                .thenReturn(new ObjetoMuseoResponseDTO(1L, "INV-1", "Casco", null, null, null, null, null, null, List.of()));

        mockMvc.perform(post("/api/admin/objetos/1/restaurar")
                        .principal(new TestingAuthenticationToken("admin-id", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(objetoMuseoService).restaurar(1L, "admin-id");
    }
}
