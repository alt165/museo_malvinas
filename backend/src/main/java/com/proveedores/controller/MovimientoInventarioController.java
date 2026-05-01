package com.proveedores.controller;

import com.proveedores.dto.MovimientoInventarioRequestDTO;
import com.proveedores.dto.MovimientoInventarioResponseDTO;
import com.proveedores.service.MovimientoInventarioService;
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

@RestController
@RequestMapping("/api/movimientos-inventario")
public class MovimientoInventarioController {

    private final MovimientoInventarioService movimientoInventarioService;

    public MovimientoInventarioController(MovimientoInventarioService movimientoInventarioService) {
        this.movimientoInventarioService = movimientoInventarioService;
    }

    @PostMapping
    public ResponseEntity<MovimientoInventarioResponseDTO> crear(@RequestBody @Valid MovimientoInventarioRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movimientoInventarioService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovimientoInventarioResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(movimientoInventarioService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<MovimientoInventarioResponseDTO>> listar() {
        return ResponseEntity.ok(movimientoInventarioService.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<MovimientoInventarioResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid MovimientoInventarioRequestDTO dto) {
        return ResponseEntity.ok(movimientoInventarioService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        movimientoInventarioService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
