package com.proveedores.controller;

import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.service.ObjetoMuseoService;
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

@Tag(name = "Objetos de museo", description = "Gestion de objetos patrimoniales del museo")
@RestController
@RequestMapping("/api/objetos")
public class ObjetoMuseoController {

    private final ObjetoMuseoService objetoMuseoService;

    public ObjetoMuseoController(ObjetoMuseoService objetoMuseoService) {
        this.objetoMuseoService = objetoMuseoService;
    }

    @Operation(summary = "Crear recurso")
    @ApiResponse(responseCode = "201", description = "Recurso creado")
    @PostMapping
    public ResponseEntity<ObjetoMuseoResponseDTO> crear(@RequestBody @Valid ObjetoMuseoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(objetoMuseoService.crear(dto));
    }

    @Operation(summary = "Obtener recurso por id")
    @ApiResponse(responseCode = "200", description = "Recurso encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<ObjetoMuseoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(objetoMuseoService.obtenerPorId(id));
    }

    @Operation(summary = "Listar recursos")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<ObjetoMuseoResponseDTO>> listar() {
        return ResponseEntity.ok(objetoMuseoService.listar());
    }

    @Operation(summary = "Actualizar recurso")
    @ApiResponse(responseCode = "200", description = "Recurso actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<ObjetoMuseoResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid ObjetoMuseoRequestDTO dto) {
        return ResponseEntity.ok(objetoMuseoService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja recurso")
    @ApiResponse(responseCode = "204", description = "Recurso dado de baja")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        objetoMuseoService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
