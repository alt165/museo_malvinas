package com.proveedores.controller;

import com.proveedores.dto.CategoriaObjetoRequestDTO;
import com.proveedores.dto.CategoriaObjetoResponseDTO;
import com.proveedores.service.CategoriaObjetoService;
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

@Tag(name = "Categorias de objetos", description = "Categorias usadas para clasificar objetos del museo")
@RestController
@RequestMapping("/api/categorias")
public class CategoriaObjetoController {

    private final CategoriaObjetoService categoriaObjetoService;

    public CategoriaObjetoController(CategoriaObjetoService categoriaObjetoService) {
        this.categoriaObjetoService = categoriaObjetoService;
    }

    @Operation(summary = "Crear recurso")
    @ApiResponse(responseCode = "201", description = "Recurso creado")
    @PostMapping
    public ResponseEntity<CategoriaObjetoResponseDTO> crear(@RequestBody @Valid CategoriaObjetoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaObjetoService.crear(dto));
    }

    @Operation(summary = "Obtener recurso por id")
    @ApiResponse(responseCode = "200", description = "Recurso encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaObjetoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaObjetoService.obtenerPorId(id));
    }

    @Operation(summary = "Listar recursos")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<CategoriaObjetoResponseDTO>> listar() {
        return ResponseEntity.ok(categoriaObjetoService.listar());
    }

    @Operation(summary = "Actualizar recurso")
    @ApiResponse(responseCode = "200", description = "Recurso actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaObjetoResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid CategoriaObjetoRequestDTO dto) {
        return ResponseEntity.ok(categoriaObjetoService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja recurso")
    @ApiResponse(responseCode = "204", description = "Recurso dado de baja")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        categoriaObjetoService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
