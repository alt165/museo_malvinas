package com.proveedores.controller;

import com.proveedores.dto.CategoriaObjetoRequestDTO;
import com.proveedores.dto.CategoriaObjetoResponseDTO;
import com.proveedores.service.CategoriaObjetoService;
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
@RequestMapping("/api/categorias")
public class CategoriaObjetoController {

    private final CategoriaObjetoService categoriaObjetoService;

    public CategoriaObjetoController(CategoriaObjetoService categoriaObjetoService) {
        this.categoriaObjetoService = categoriaObjetoService;
    }

    @PostMapping
    public ResponseEntity<CategoriaObjetoResponseDTO> crear(@RequestBody @Valid CategoriaObjetoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaObjetoService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaObjetoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaObjetoService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<CategoriaObjetoResponseDTO>> listar() {
        return ResponseEntity.ok(categoriaObjetoService.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaObjetoResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid CategoriaObjetoRequestDTO dto) {
        return ResponseEntity.ok(categoriaObjetoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        categoriaObjetoService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
