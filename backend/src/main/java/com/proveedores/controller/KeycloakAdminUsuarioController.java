package com.proveedores.controller;

import com.proveedores.dto.AsignarRolRequestDTO;
import com.proveedores.dto.ResetPasswordRequestDTO;
import com.proveedores.dto.UsuarioKeycloakRequestDTO;
import com.proveedores.dto.UsuarioKeycloakResponseDTO;
import com.proveedores.service.KeycloakAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Administracion de usuarios", description = "Gestion de usuarios reales en Keycloak")
@RestController
@RequestMapping("/api/admin/usuarios")
public class KeycloakAdminUsuarioController {

    private final KeycloakAdminService keycloakAdminService;

    public KeycloakAdminUsuarioController(KeycloakAdminService keycloakAdminService) {
        this.keycloakAdminService = keycloakAdminService;
    }

    @Operation(summary = "Listar usuarios de Keycloak")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<UsuarioKeycloakResponseDTO>> listar() {
        return ResponseEntity.ok(keycloakAdminService.listarUsuarios());
    }

    @Operation(summary = "Obtener usuario de Keycloak por id")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioKeycloakResponseDTO> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(keycloakAdminService.obtenerUsuario(id));
    }

    @Operation(summary = "Crear usuario en Keycloak")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @PostMapping
    public ResponseEntity<UsuarioKeycloakResponseDTO> crear(@RequestBody @Valid UsuarioKeycloakRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(keycloakAdminService.crearUsuario(dto));
    }

    @Operation(summary = "Actualizar datos basicos de usuario en Keycloak")
    @ApiResponse(responseCode = "200", description = "Usuario actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioKeycloakResponseDTO> actualizar(
            @PathVariable String id,
            @RequestBody @Valid UsuarioKeycloakRequestDTO dto
    ) {
        return ResponseEntity.ok(keycloakAdminService.actualizarDatosBasicos(id, dto));
    }

    @Operation(summary = "Habilitar o deshabilitar usuario en Keycloak")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioKeycloakResponseDTO> cambiarEstado(
            @PathVariable String id,
            @RequestParam boolean habilitado
    ) {
        return ResponseEntity.ok(keycloakAdminService.cambiarEstado(id, habilitado));
    }

    @Operation(summary = "Resetear contrasena temporal de usuario en Keycloak")
    @ApiResponse(responseCode = "204", description = "Contrasena temporal configurada")
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetearContrasena(
            @PathVariable String id,
            @RequestBody @Valid ResetPasswordRequestDTO dto
    ) {
        keycloakAdminService.resetearContrasenaTemporal(id, dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Asignar roles gestionados a usuario en Keycloak")
    @ApiResponse(responseCode = "200", description = "Roles actualizados")
    @PutMapping("/{id}/roles")
    public ResponseEntity<UsuarioKeycloakResponseDTO> asignarRoles(
            @PathVariable String id,
            @RequestBody @Valid AsignarRolRequestDTO dto,
            Authentication authentication
    ) {
        String administradorActualId = authentication == null ? null : authentication.getName();
        return ResponseEntity.ok(keycloakAdminService.asignarRoles(id, dto, administradorActualId));
    }
}
