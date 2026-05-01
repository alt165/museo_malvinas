package com.proveedores.controller;

import com.proveedores.dto.VeteranoRequestDTO;
import com.proveedores.dto.VeteranoResponseDTO;
import com.proveedores.service.VeteranoService;
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

@Tag(name = "Veteranos", description = "Gestion de veteranos relacionados al museo")
@RestController
@RequestMapping("/api/veteranos")
public class VeteranoController {

    private final VeteranoService veteranoService;

    public VeteranoController(VeteranoService veteranoService) {
        this.veteranoService = veteranoService;
    }

    @Operation(summary = "Crear recurso")
    @ApiResponse(responseCode = "201", description = "Recurso creado")
    @PostMapping
    public ResponseEntity<VeteranoResponseDTO> crear(@RequestBody @Valid VeteranoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(veteranoService.crear(dto));
    }

    @Operation(summary = "Obtener recurso por id")
    @ApiResponse(responseCode = "200", description = "Recurso encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<VeteranoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(veteranoService.obtenerPorId(id));
    }

    @Operation(summary = "Listar recursos")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<VeteranoResponseDTO>> listar() {
        return ResponseEntity.ok(veteranoService.listar());
    }

    @Operation(summary = "Actualizar recurso")
    @ApiResponse(responseCode = "200", description = "Recurso actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<VeteranoResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid VeteranoRequestDTO dto) {
        return ResponseEntity.ok(veteranoService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja recurso")
    @ApiResponse(responseCode = "204", description = "Recurso dado de baja")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        veteranoService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
