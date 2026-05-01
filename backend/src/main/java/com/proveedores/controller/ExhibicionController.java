package com.proveedores.controller;

import com.proveedores.dto.ExhibicionRequestDTO;
import com.proveedores.dto.ExhibicionResponseDTO;
import com.proveedores.service.ExhibicionService;
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

@Tag(name = "Exhibiciones", description = "Gestion de exhibiciones del museo")
@RestController
@RequestMapping("/api/exhibiciones")
public class ExhibicionController {

    private final ExhibicionService exhibicionService;

    public ExhibicionController(ExhibicionService exhibicionService) {
        this.exhibicionService = exhibicionService;
    }

    @Operation(summary = "Crear recurso")
    @ApiResponse(responseCode = "201", description = "Recurso creado")
    @PostMapping
    public ResponseEntity<ExhibicionResponseDTO> crear(@RequestBody @Valid ExhibicionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exhibicionService.crear(dto));
    }

    @Operation(summary = "Obtener recurso por id")
    @ApiResponse(responseCode = "200", description = "Recurso encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<ExhibicionResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(exhibicionService.obtenerPorId(id));
    }

    @Operation(summary = "Listar recursos")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<ExhibicionResponseDTO>> listar() {
        return ResponseEntity.ok(exhibicionService.listar());
    }

    @Operation(summary = "Actualizar recurso")
    @ApiResponse(responseCode = "200", description = "Recurso actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<ExhibicionResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid ExhibicionRequestDTO dto) {
        return ResponseEntity.ok(exhibicionService.actualizar(id, dto));
    }

    @Operation(summary = "Finalizar exhibicion")
    @ApiResponse(responseCode = "200", description = "Exhibicion finalizada")
    @PostMapping("/{id}/finalizar")
    public ResponseEntity<ExhibicionResponseDTO> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(exhibicionService.finalizar(id));
    }

    @Operation(summary = "Dar de baja recurso")
    @ApiResponse(responseCode = "204", description = "Recurso dado de baja")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        exhibicionService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
