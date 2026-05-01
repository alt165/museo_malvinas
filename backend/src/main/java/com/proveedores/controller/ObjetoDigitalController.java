package com.proveedores.controller;

import com.proveedores.dto.ObjetoDigitalRequestDTO;
import com.proveedores.dto.ObjetoDigitalResponseDTO;
import com.proveedores.service.ObjetoDigitalService;
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

@Tag(name = "Objetos digitales", description = "Gestion de objetos digitales del museo")
@RestController
@RequestMapping("/api/objetos-digitales")
public class ObjetoDigitalController {

    private final ObjetoDigitalService objetoDigitalService;

    public ObjetoDigitalController(ObjetoDigitalService objetoDigitalService) {
        this.objetoDigitalService = objetoDigitalService;
    }

    @Operation(summary = "Crear recurso")
    @ApiResponse(responseCode = "201", description = "Recurso creado")
    @PostMapping
    public ResponseEntity<ObjetoDigitalResponseDTO> crear(@RequestBody @Valid ObjetoDigitalRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(objetoDigitalService.crear(dto));
    }

    @Operation(summary = "Obtener recurso por id")
    @ApiResponse(responseCode = "200", description = "Recurso encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<ObjetoDigitalResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(objetoDigitalService.obtenerPorId(id));
    }

    @Operation(summary = "Listar recursos")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<ObjetoDigitalResponseDTO>> listar() {
        return ResponseEntity.ok(objetoDigitalService.listar());
    }

    @Operation(summary = "Actualizar recurso")
    @ApiResponse(responseCode = "200", description = "Recurso actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<ObjetoDigitalResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid ObjetoDigitalRequestDTO dto) {
        return ResponseEntity.ok(objetoDigitalService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja recurso")
    @ApiResponse(responseCode = "204", description = "Recurso dado de baja")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        objetoDigitalService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
