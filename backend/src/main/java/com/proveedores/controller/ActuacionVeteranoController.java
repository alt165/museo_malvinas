package com.proveedores.controller;

import com.proveedores.dto.ActuacionVeteranoRequestDTO;
import com.proveedores.dto.ActuacionVeteranoResponseDTO;
import com.proveedores.service.ActuacionVeteranoService;
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

@Tag(name = "Actuaciones de veteranos", description = "Actuaciones de veteranos durante la guerra")
@RestController
@RequestMapping("/api/actuaciones-veteranos")
public class ActuacionVeteranoController {

    private final ActuacionVeteranoService actuacionVeteranoService;

    public ActuacionVeteranoController(ActuacionVeteranoService actuacionVeteranoService) {
        this.actuacionVeteranoService = actuacionVeteranoService;
    }

    @Operation(summary = "Crear recurso")
    @ApiResponse(responseCode = "201", description = "Recurso creado")
    @PostMapping
    public ResponseEntity<ActuacionVeteranoResponseDTO> crear(@RequestBody @Valid ActuacionVeteranoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(actuacionVeteranoService.crear(dto));
    }

    @Operation(summary = "Obtener recurso por id")
    @ApiResponse(responseCode = "200", description = "Recurso encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<ActuacionVeteranoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(actuacionVeteranoService.obtenerPorId(id));
    }

    @Operation(summary = "Listar recursos")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<ActuacionVeteranoResponseDTO>> listar() {
        return ResponseEntity.ok(actuacionVeteranoService.listar());
    }

    @Operation(summary = "Actualizar recurso")
    @ApiResponse(responseCode = "200", description = "Recurso actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<ActuacionVeteranoResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid ActuacionVeteranoRequestDTO dto) {
        return ResponseEntity.ok(actuacionVeteranoService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja recurso")
    @ApiResponse(responseCode = "204", description = "Recurso dado de baja")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        actuacionVeteranoService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
