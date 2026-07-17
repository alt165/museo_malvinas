package com.proveedores.controller;

import com.proveedores.dto.UnidadMilitarRequestDTO;
import com.proveedores.dto.UnidadMilitarResponseDTO;
import com.proveedores.entity.Fuerza;
import com.proveedores.service.UnidadMilitarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Unidades militares", description = "Catalogo de unidades militares por fuerza")
@RestController
@RequestMapping("/api/unidades-militares")
public class UnidadMilitarController {

    private final UnidadMilitarService unidadMilitarService;

    public UnidadMilitarController(UnidadMilitarService unidadMilitarService) {
        this.unidadMilitarService = unidadMilitarService;
    }

    @Operation(summary = "Buscar unidades militares activas")
    @ApiResponse(responseCode = "200", description = "Busqueda obtenida")
    @GetMapping
    public ResponseEntity<List<UnidadMilitarResponseDTO>> listar(
            @RequestParam(required = false) Fuerza fuerza,
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Integer limite
    ) {
        if (fuerza == null) {
            return ResponseEntity.ok(unidadMilitarService.listar());
        }
        return ResponseEntity.ok(unidadMilitarService.buscarPorFuerza(fuerza, buscar, limite));
    }

    @Operation(summary = "Obtener unidad militar por id")
    @ApiResponse(responseCode = "200", description = "Unidad encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<UnidadMilitarResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(unidadMilitarService.obtenerPorId(id));
    }

    @Operation(summary = "Crear unidad militar")
    @ApiResponse(responseCode = "201", description = "Unidad creada")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UnidadMilitarResponseDTO> crear(@RequestBody @Valid UnidadMilitarRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(unidadMilitarService.crear(dto));
    }

    @Operation(summary = "Actualizar unidad militar")
    @ApiResponse(responseCode = "200", description = "Unidad actualizada")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UnidadMilitarResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid UnidadMilitarRequestDTO dto) {
        return ResponseEntity.ok(unidadMilitarService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja unidad militar")
    @ApiResponse(responseCode = "204", description = "Unidad dada de baja")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        unidadMilitarService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
