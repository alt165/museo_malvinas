package com.proveedores.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proveedores.dto.AsignarRolRequestDTO;
import com.proveedores.dto.ResetPasswordRequestDTO;
import com.proveedores.dto.UsuarioKeycloakRequestDTO;
import com.proveedores.dto.UsuarioKeycloakResponseDTO;
import com.proveedores.exception.GlobalExceptionHandler;
import com.proveedores.security.KeycloakJwtAuthenticationConverter;
import com.proveedores.service.KeycloakAdminService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(KeycloakAdminUsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class KeycloakAdminUsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakAdminService keycloakAdminService;

    @MockBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Test
    void listarUsuariosDevuelveListado() throws Exception {
        when(keycloakAdminService.listarUsuarios()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("user-id"))
                .andExpect(jsonPath("$[0].roles[0]").value("ADMIN"));
    }

    @Test
    void crearUsuarioDevuelveCreated() throws Exception {
        UsuarioKeycloakRequestDTO request = new UsuarioKeycloakRequestDTO(
                "admin",
                "admin@local.test",
                "12345678",
                "Admin",
                "Local",
                true,
                "Temporal123",
                Set.of("ADMIN")
        );
        when(keycloakAdminService.crearUsuario(any(UsuarioKeycloakRequestDTO.class))).thenReturn(response());

        mockMvc.perform(post("/api/admin/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("user-id"))
                .andExpect(jsonPath("$.dni").value("12345678"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
    }

    @Test
    void crearUsuarioSinDniDevuelveBadRequest() throws Exception {
        String request = """
                {
                  "username": "admin",
                  "email": "admin@local.test",
                  "nombre": "Admin",
                  "apellido": "Local",
                  "habilitado": true,
                  "roles": ["ADMIN"]
                }
                """;

        mockMvc.perform(post("/api/admin/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.dni").value("El DNI es obligatorio"));
    }

    @Test
    void actualizarUsuarioDevuelveOk() throws Exception {
        UsuarioKeycloakRequestDTO request = new UsuarioKeycloakRequestDTO(
                "admin",
                "admin@local.test",
                "87654321",
                "Admin",
                "Local",
                true,
                null,
                Set.of("ADMIN")
        );
        when(keycloakAdminService.actualizarDatosBasicos(eq("user-id"), any(UsuarioKeycloakRequestDTO.class))).thenReturn(response());

        mockMvc.perform(put("/api/admin/usuarios/user-id")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.dni").value("12345678"));
    }

    @Test
    void cambiarEstadoDevuelveOk() throws Exception {
        when(keycloakAdminService.cambiarEstado("user-id", false)).thenReturn(response());

        mockMvc.perform(patch("/api/admin/usuarios/user-id/estado")
                        .param("habilitado", "false"))
                .andExpect(status().isOk());
    }

    @Test
    void resetearContrasenaDevuelveNoContent() throws Exception {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO("Temporal123");

        mockMvc.perform(post("/api/admin/usuarios/user-id/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(keycloakAdminService).resetearContrasenaTemporal(eq("user-id"), any(ResetPasswordRequestDTO.class));
    }

    @Test
    void asignarRolesPasaAdministradorActualAlServicio() throws Exception {
        AsignarRolRequestDTO request = new AsignarRolRequestDTO(Set.of("VIEWER"), true);
        when(keycloakAdminService.asignarRoles(eq("user-id"), any(AsignarRolRequestDTO.class), eq("admin-id")))
                .thenReturn(response());

        mockMvc.perform(put("/api/admin/usuarios/user-id/roles")
                        .principal(new TestingAuthenticationToken("admin-id", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private UsuarioKeycloakResponseDTO response() {
        return new UsuarioKeycloakResponseDTO(
                "user-id",
                "admin",
                "admin@local.test",
                "12345678",
                "Admin",
                "Local",
                true,
                Set.of("ADMIN")
        );
    }
}
