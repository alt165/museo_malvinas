package com.proveedores.controller;

import com.proveedores.dto.ExhibicionRequestDTO;
import com.proveedores.dto.ExhibicionResponseDTO;
import com.proveedores.service.ExhibicionService;
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
@RequestMapping("/api/exhibiciones")
public class ExhibicionController {

    private final ExhibicionService exhibicionService;

    public ExhibicionController(ExhibicionService exhibicionService) {
        this.exhibicionService = exhibicionService;
    }

    @PostMapping
    public ResponseEntity<ExhibicionResponseDTO> crear(@RequestBody @Valid ExhibicionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exhibicionService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExhibicionResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(exhibicionService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<ExhibicionResponseDTO>> listar() {
        return ResponseEntity.ok(exhibicionService.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExhibicionResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid ExhibicionRequestDTO dto) {
        return ResponseEntity.ok(exhibicionService.actualizar(id, dto));
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<ExhibicionResponseDTO> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(exhibicionService.finalizar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        exhibicionService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
