package com.proveedores.controller;

import com.proveedores.dto.RelacionObjetoRequestDTO;
import com.proveedores.dto.RelacionObjetoResponseDTO;
import com.proveedores.service.RelacionObjetoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Relaciones entre objetos", description = "Relaciones direccionales entre objetos del museo")
@RestController
@RequestMapping("/api/relaciones-objetos")
public class RelacionObjetoController {

    private final RelacionObjetoService relacionObjetoService;

    public RelacionObjetoController(RelacionObjetoService relacionObjetoService) {
        this.relacionObjetoService = relacionObjetoService;
    }

    @Operation(summary = "Crear recurso")
    @ApiResponse(responseCode = "201", description = "Recurso creado")
    @PostMapping
    public ResponseEntity<RelacionObjetoResponseDTO> crear(@RequestBody @Valid RelacionObjetoRequestDTO dto, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(relacionObjetoService.crear(dto, usuario(authentication)));
    }

    @Operation(summary = "Obtener recurso por id")
    @ApiResponse(responseCode = "200", description = "Recurso encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<RelacionObjetoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(relacionObjetoService.obtenerPorId(id));
    }

    @Operation(summary = "Listar recursos")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<RelacionObjetoResponseDTO>> listar() {
        return ResponseEntity.ok(relacionObjetoService.listar());
    }

    @Operation(summary = "Actualizar recurso")
    @ApiResponse(responseCode = "200", description = "Recurso actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<RelacionObjetoResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid RelacionObjetoRequestDTO dto) {
        return ResponseEntity.ok(relacionObjetoService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja recurso")
    @ApiResponse(responseCode = "204", description = "Recurso dado de baja")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        relacionObjetoService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }

    private String usuario(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            String preferredUsername = jwtAuthentication.getToken().getClaimAsString("preferred_username");
            if (StringUtils.hasText(preferredUsername)) {
                return preferredUsername;
            }
        }
        return authentication == null ? null : authentication.getName();
    }
}
