package com.proveedores.controller;

import com.proveedores.dto.RangoMilitarRequestDTO;
import com.proveedores.dto.RangoMilitarResponseDTO;
import com.proveedores.entity.Fuerza;
import com.proveedores.service.RangoMilitarService;
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

@Tag(name = "Rangos militares", description = "Catalogo de rangos militares por fuerza")
@RestController
@RequestMapping("/api/rangos-militares")
public class RangoMilitarController {

    private final RangoMilitarService rangoMilitarService;

    public RangoMilitarController(RangoMilitarService rangoMilitarService) {
        this.rangoMilitarService = rangoMilitarService;
    }

    @Operation(summary = "Listar rangos activos")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<RangoMilitarResponseDTO>> listar(@RequestParam(required = false) Fuerza fuerza) {
        if (fuerza == null) {
            return ResponseEntity.ok(rangoMilitarService.listar());
        }
        return ResponseEntity.ok(rangoMilitarService.listarPorFuerza(fuerza));
    }

    @Operation(summary = "Obtener rango militar por id")
    @ApiResponse(responseCode = "200", description = "Rango encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<RangoMilitarResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(rangoMilitarService.obtenerPorId(id));
    }

    @Operation(summary = "Crear rango militar")
    @ApiResponse(responseCode = "201", description = "Rango creado")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<RangoMilitarResponseDTO> crear(@RequestBody @Valid RangoMilitarRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rangoMilitarService.crear(dto));
    }

    @Operation(summary = "Actualizar rango militar")
    @ApiResponse(responseCode = "200", description = "Rango actualizado")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<RangoMilitarResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid RangoMilitarRequestDTO dto) {
        return ResponseEntity.ok(rangoMilitarService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja rango militar")
    @ApiResponse(responseCode = "204", description = "Rango dado de baja")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        rangoMilitarService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
