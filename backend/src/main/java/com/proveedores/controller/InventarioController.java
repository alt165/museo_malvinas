package com.proveedores.controller;

import com.proveedores.dto.InventarioRequestDTO;
import com.proveedores.dto.InventarioResponseDTO;
import com.proveedores.service.InventarioService;
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
@RequestMapping("/api/inventarios")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @PostMapping
    public ResponseEntity<InventarioResponseDTO> crear(@RequestBody @Valid InventarioRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventarioResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(inventarioService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<InventarioResponseDTO>> listar() {
        return ResponseEntity.ok(inventarioService.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventarioResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid InventarioRequestDTO dto) {
        return ResponseEntity.ok(inventarioService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        inventarioService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
