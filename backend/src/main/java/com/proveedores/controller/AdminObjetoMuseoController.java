package com.proveedores.controller;

import com.proveedores.dto.ObjetoMuseoEliminadoResponseDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.service.ObjetoMuseoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Administracion de objetos", description = "Gestion administrativa de objetos eliminados")
@RestController
@RequestMapping("/api/admin/objetos")
@PreAuthorize("hasRole('ADMIN')")
public class AdminObjetoMuseoController {

    private final ObjetoMuseoService objetoMuseoService;

    public AdminObjetoMuseoController(ObjetoMuseoService objetoMuseoService) {
        this.objetoMuseoService = objetoMuseoService;
    }

    @Operation(summary = "Listar objetos eliminados logicamente")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping("/eliminados")
    public ResponseEntity<Page<ObjetoMuseoEliminadoResponseDTO>> listarEliminados(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(objetoMuseoService.listarEliminados(pageable));
    }

    @Operation(summary = "Restaurar objeto eliminado logicamente")
    @ApiResponse(responseCode = "200", description = "Objeto restaurado")
    @PostMapping("/{id}/restaurar")
    public ResponseEntity<ObjetoMuseoResponseDTO> restaurar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(objetoMuseoService.restaurar(id, usuario(authentication)));
    }

    private String usuario(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            String username = jwtAuthentication.getToken().getClaimAsString("preferred_username");
            if (StringUtils.hasText(username)) {
                return username;
            }
        }
        return authentication.getName();
    }
}
