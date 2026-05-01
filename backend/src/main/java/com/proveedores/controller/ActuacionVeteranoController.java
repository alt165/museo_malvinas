package com.proveedores.controller;

import com.proveedores.dto.ActuacionVeteranoRequestDTO;
import com.proveedores.dto.ActuacionVeteranoResponseDTO;
import com.proveedores.service.ActuacionVeteranoService;
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
@RequestMapping("/api/actuaciones-veteranos")
public class ActuacionVeteranoController {

    private final ActuacionVeteranoService actuacionVeteranoService;

    public ActuacionVeteranoController(ActuacionVeteranoService actuacionVeteranoService) {
        this.actuacionVeteranoService = actuacionVeteranoService;
    }

    @PostMapping
    public ResponseEntity<ActuacionVeteranoResponseDTO> crear(@RequestBody @Valid ActuacionVeteranoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(actuacionVeteranoService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActuacionVeteranoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(actuacionVeteranoService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<ActuacionVeteranoResponseDTO>> listar() {
        return ResponseEntity.ok(actuacionVeteranoService.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActuacionVeteranoResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid ActuacionVeteranoRequestDTO dto) {
        return ResponseEntity.ok(actuacionVeteranoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        actuacionVeteranoService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
