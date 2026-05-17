package com.proveedores.controller;

import com.proveedores.dto.AgregarObjetosColeccionRequestDTO;
import com.proveedores.dto.ColeccionObjetoRequestDTO;
import com.proveedores.dto.ColeccionObjetoResponseDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.service.ColeccionObjetoService;
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

@Tag(name = "Colecciones de objetos", description = "Gestion de colecciones de objetos patrimoniales")
@RestController
@RequestMapping("/api/colecciones")
public class ColeccionObjetoController {

    private final ColeccionObjetoService coleccionObjetoService;

    public ColeccionObjetoController(ColeccionObjetoService coleccionObjetoService) {
        this.coleccionObjetoService = coleccionObjetoService;
    }

    @Operation(summary = "Crear coleccion")
    @ApiResponse(responseCode = "201", description = "Coleccion creada")
    @PostMapping
    public ResponseEntity<ColeccionObjetoResponseDTO> crear(@RequestBody @Valid ColeccionObjetoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(coleccionObjetoService.crear(dto));
    }

    @Operation(summary = "Listar colecciones")
    @GetMapping
    public ResponseEntity<List<ColeccionObjetoResponseDTO>> listar() {
        return ResponseEntity.ok(coleccionObjetoService.listar());
    }

    @Operation(summary = "Obtener coleccion por id")
    @GetMapping("/{id}")
    public ResponseEntity<ColeccionObjetoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(coleccionObjetoService.obtenerPorId(id));
    }

    @Operation(summary = "Actualizar coleccion")
    @PutMapping("/{id}")
    public ResponseEntity<ColeccionObjetoResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid ColeccionObjetoRequestDTO dto) {
        return ResponseEntity.ok(coleccionObjetoService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja coleccion")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        coleccionObjetoService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar objetos de una coleccion")
    @GetMapping("/{id}/objetos")
    public ResponseEntity<List<ObjetoMuseoResponseDTO>> listarObjetos(@PathVariable Long id) {
        return ResponseEntity.ok(coleccionObjetoService.listarObjetos(id));
    }

    @Operation(summary = "Agregar objetos a una coleccion")
    @PostMapping("/{id}/objetos")
    public ResponseEntity<List<ObjetoMuseoResponseDTO>> agregarObjetos(
            @PathVariable Long id,
            @RequestBody @Valid AgregarObjetosColeccionRequestDTO dto
    ) {
        return ResponseEntity.ok(coleccionObjetoService.agregarObjetos(id, dto));
    }

    @Operation(summary = "Quitar objeto de una coleccion")
    @DeleteMapping("/{id}/objetos/{objetoId}")
    public ResponseEntity<Void> quitarObjeto(@PathVariable Long id, @PathVariable Long objetoId) {
        coleccionObjetoService.quitarObjeto(id, objetoId);
        return ResponseEntity.noContent().build();
    }
}
