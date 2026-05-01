package com.proveedores.controller;

import com.proveedores.dto.MovimientoInventarioRequestDTO;
import com.proveedores.dto.MovimientoInventarioResponseDTO;
import com.proveedores.service.MovimientoInventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Movimientos de inventario", description = "Historial de movimientos de inventario")
@RestController
@RequestMapping("/api/movimientos-inventario")
public class MovimientoInventarioController {

    private final MovimientoInventarioService movimientoInventarioService;

    public MovimientoInventarioController(MovimientoInventarioService movimientoInventarioService) {
        this.movimientoInventarioService = movimientoInventarioService;
    }

    @Operation(summary = "Crear recurso")
    @ApiResponse(responseCode = "201", description = "Recurso creado")
    @PostMapping
    public ResponseEntity<MovimientoInventarioResponseDTO> crear(@RequestBody @Valid MovimientoInventarioRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movimientoInventarioService.crear(dto));
    }

    @Operation(summary = "Obtener recurso por id")
    @ApiResponse(responseCode = "200", description = "Recurso encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<MovimientoInventarioResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(movimientoInventarioService.obtenerPorId(id));
    }

    @Operation(summary = "Listar recursos")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<MovimientoInventarioResponseDTO>> listar() {
        return ResponseEntity.ok(movimientoInventarioService.listar());
    }

    @Operation(summary = "Actualizar recurso")
    @ApiResponse(responseCode = "200", description = "Recurso actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<MovimientoInventarioResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid MovimientoInventarioRequestDTO dto) {
        return ResponseEntity.ok(movimientoInventarioService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja recurso")
    @ApiResponse(responseCode = "204", description = "Recurso dado de baja")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        movimientoInventarioService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
