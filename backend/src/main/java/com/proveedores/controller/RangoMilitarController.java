package com.proveedores.controller;

import com.proveedores.dto.RangoMilitarResponseDTO;
import com.proveedores.entity.Fuerza;
import com.proveedores.service.RangoMilitarService;
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

@Tag(name = "Rangos militares", description = "Catalogo de rangos militares por fuerza")
@RestController
@RequestMapping("/api/rangos-militares")
public class RangoMilitarController {

    private final RangoMilitarService rangoMilitarService;

    public RangoMilitarController(RangoMilitarService rangoMilitarService) {
        this.rangoMilitarService = rangoMilitarService;
    }

    @Operation(summary = "Listar rangos activos por fuerza")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<RangoMilitarResponseDTO>> listar(@RequestParam Fuerza fuerza) {
        return ResponseEntity.ok(rangoMilitarService.listarPorFuerza(fuerza));
    }

    @Operation(summary = "Obtener rango militar por id")
    @ApiResponse(responseCode = "200", description = "Rango encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<RangoMilitarResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(rangoMilitarService.obtenerPorId(id));
    }
}
