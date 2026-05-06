package com.proveedores.controller;

import com.proveedores.dto.ExhibicionObjetoRequestDTO;
import com.proveedores.dto.ExhibicionObjetoResponseDTO;
import com.proveedores.service.ExhibicionObjetoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Objetos en exhibiciones", description = "Asignacion y devolucion de objetos en exhibiciones")
@RestController
@RequestMapping("/api/exhibiciones-objetos")
public class ExhibicionObjetoController {

    private final ExhibicionObjetoService exhibicionObjetoService;

    public ExhibicionObjetoController(ExhibicionObjetoService exhibicionObjetoService) {
        this.exhibicionObjetoService = exhibicionObjetoService;
    }

    @Operation(summary = "Crear recurso")
    @ApiResponse(responseCode = "201", description = "Recurso creado")
    @PostMapping
    public ResponseEntity<ExhibicionObjetoResponseDTO> crear(@RequestBody @Valid ExhibicionObjetoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exhibicionObjetoService.crear(dto));
    }

    @Operation(summary = "Obtener recurso por id")
    @ApiResponse(responseCode = "200", description = "Recurso encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<ExhibicionObjetoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(exhibicionObjetoService.obtenerPorId(id));
    }

    @Operation(summary = "Listar recursos")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<ExhibicionObjetoResponseDTO>> listar() {
        return ResponseEntity.ok(exhibicionObjetoService.listar());
    }

    @Operation(summary = "Actualizar recurso")
    @ApiResponse(responseCode = "200", description = "Recurso actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<ExhibicionObjetoResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid ExhibicionObjetoRequestDTO dto) {
        return ResponseEntity.ok(exhibicionObjetoService.actualizar(id, dto));
    }

    @Operation(summary = "Verificar devolucion de objeto")
    @ApiResponse(responseCode = "200", description = "Devolucion verificada")
    @PostMapping("/{id}/verificar-devolucion")
    public ResponseEntity<ExhibicionObjetoResponseDTO> verificarDevolucion(
            @PathVariable Long id,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) String observaciones
    ) {
        return ResponseEntity.ok(exhibicionObjetoService.verificarDevolucion(id, usuarioId, observaciones));
    }

    @Operation(summary = "Revertir verificacion de devolucion de objeto")
    @ApiResponse(responseCode = "200", description = "Devolucion revertida")
    @PostMapping("/{id}/revertir-devolucion")
    public ResponseEntity<ExhibicionObjetoResponseDTO> revertirDevolucion(@PathVariable Long id) {
        return ResponseEntity.ok(exhibicionObjetoService.revertirDevolucion(id));
    }

    @Operation(summary = "Dar de baja recurso")
    @ApiResponse(responseCode = "204", description = "Recurso dado de baja")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        exhibicionObjetoService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
