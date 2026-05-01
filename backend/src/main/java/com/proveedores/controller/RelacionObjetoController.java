package com.proveedores.controller;

import com.proveedores.dto.RelacionObjetoRequestDTO;
import com.proveedores.dto.RelacionObjetoResponseDTO;
import com.proveedores.service.RelacionObjetoService;
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
@RequestMapping("/api/relaciones-objetos")
public class RelacionObjetoController {

    private final RelacionObjetoService relacionObjetoService;

    public RelacionObjetoController(RelacionObjetoService relacionObjetoService) {
        this.relacionObjetoService = relacionObjetoService;
    }

    @PostMapping
    public ResponseEntity<RelacionObjetoResponseDTO> crear(@RequestBody @Valid RelacionObjetoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(relacionObjetoService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RelacionObjetoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(relacionObjetoService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<RelacionObjetoResponseDTO>> listar() {
        return ResponseEntity.ok(relacionObjetoService.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<RelacionObjetoResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid RelacionObjetoRequestDTO dto) {
        return ResponseEntity.ok(relacionObjetoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        relacionObjetoService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
