package com.proveedores.controller;

import com.proveedores.dto.ObjetoVeteranoRequestDTO;
import com.proveedores.dto.ObjetoVeteranoResponseDTO;
import com.proveedores.service.ObjetoVeteranoService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Objetos de veteranos", description = "Relacion entre objetos del museo y veteranos")
@RestController
@RequestMapping("/api/objetos-veteranos")
public class ObjetoVeteranoController {

    private final ObjetoVeteranoService objetoVeteranoService;

    public ObjetoVeteranoController(ObjetoVeteranoService objetoVeteranoService) {
        this.objetoVeteranoService = objetoVeteranoService;
    }

    @Operation(summary = "Crear relacion objeto-veterano")
    @ApiResponse(responseCode = "201", description = "Relacion creada")
    @PostMapping
    public ResponseEntity<ObjetoVeteranoResponseDTO> crear(@RequestBody @Valid ObjetoVeteranoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(objetoVeteranoService.crear(dto));
    }

    @Operation(summary = "Obtener relacion por id")
    @ApiResponse(responseCode = "200", description = "Relacion encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<ObjetoVeteranoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(objetoVeteranoService.obtenerPorId(id));
    }

    @Operation(summary = "Listar relaciones")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<ObjetoVeteranoResponseDTO>> listar(@RequestParam(required = false) Long veteranoId) {
        if (veteranoId != null) {
            return ResponseEntity.ok(objetoVeteranoService.listarPorVeterano(veteranoId));
        }
        return ResponseEntity.ok(objetoVeteranoService.listar());
    }

    @Operation(summary = "Dar de baja relacion")
    @ApiResponse(responseCode = "204", description = "Relacion dada de baja")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        objetoVeteranoService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
