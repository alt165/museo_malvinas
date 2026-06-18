package com.proveedores.controller;

import com.proveedores.dto.UnidadMilitarResponseDTO;
import com.proveedores.entity.Fuerza;
import com.proveedores.service.UnidadMilitarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @Operation(summary = "Buscar unidades militares activas por fuerza")
    @ApiResponse(responseCode = "200", description = "Busqueda obtenida")
    @GetMapping
    public ResponseEntity<List<UnidadMilitarResponseDTO>> listar(
            @RequestParam Fuerza fuerza,
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Integer limite
    ) {
        return ResponseEntity.ok(unidadMilitarService.buscarPorFuerza(fuerza, buscar, limite));
    }

    @Operation(summary = "Obtener unidad militar por id")
    @ApiResponse(responseCode = "200", description = "Unidad encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<UnidadMilitarResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(unidadMilitarService.obtenerPorId(id));
    }
}
