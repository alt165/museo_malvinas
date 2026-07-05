package com.proveedores.controller;

import com.proveedores.dto.DetalleConservacionRequestDTO;
import com.proveedores.dto.DetalleConservacionResponseDTO;
import com.proveedores.service.DetalleConservacionService;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Detalles de conservacion", description = "Catalogo de detalles del estado de conservacion")
@RestController
@RequestMapping("/api/detalles-conservacion")
public class DetalleConservacionController {

    private final DetalleConservacionService detalleConservacionService;

    public DetalleConservacionController(DetalleConservacionService detalleConservacionService) {
        this.detalleConservacionService = detalleConservacionService;
    }

    @Operation(summary = "Listar detalles activos")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<DetalleConservacionResponseDTO>> listar() {
        return ResponseEntity.ok(detalleConservacionService.listar());
    }

    @Operation(summary = "Obtener detalle por id")
    @ApiResponse(responseCode = "200", description = "Detalle encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<DetalleConservacionResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(detalleConservacionService.obtenerPorId(id));
    }

    @Operation(summary = "Crear detalle")
    @ApiResponse(responseCode = "201", description = "Detalle creado")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<DetalleConservacionResponseDTO> crear(@RequestBody @Valid DetalleConservacionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(detalleConservacionService.crear(dto));
    }

    @Operation(summary = "Actualizar detalle")
    @ApiResponse(responseCode = "200", description = "Detalle actualizado")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<DetalleConservacionResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid DetalleConservacionRequestDTO dto) {
        return ResponseEntity.ok(detalleConservacionService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja detalle")
    @ApiResponse(responseCode = "204", description = "Detalle dado de baja")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        detalleConservacionService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
