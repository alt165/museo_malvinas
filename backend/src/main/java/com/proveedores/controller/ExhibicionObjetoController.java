package com.proveedores.controller;

import com.proveedores.dto.ExhibicionObjetoRequestDTO;
import com.proveedores.dto.ExhibicionObjetoResponseDTO;
import com.proveedores.service.ExhibicionObjetoService;
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

@RestController
@RequestMapping("/api/exhibiciones-objetos")
public class ExhibicionObjetoController {

    private final ExhibicionObjetoService exhibicionObjetoService;

    public ExhibicionObjetoController(ExhibicionObjetoService exhibicionObjetoService) {
        this.exhibicionObjetoService = exhibicionObjetoService;
    }

    @PostMapping
    public ResponseEntity<ExhibicionObjetoResponseDTO> crear(@RequestBody @Valid ExhibicionObjetoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exhibicionObjetoService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExhibicionObjetoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(exhibicionObjetoService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<ExhibicionObjetoResponseDTO>> listar() {
        return ResponseEntity.ok(exhibicionObjetoService.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExhibicionObjetoResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid ExhibicionObjetoRequestDTO dto) {
        return ResponseEntity.ok(exhibicionObjetoService.actualizar(id, dto));
    }

    @PostMapping("/{id}/verificar-devolucion")
    public ResponseEntity<ExhibicionObjetoResponseDTO> verificarDevolucion(
            @PathVariable Long id,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) String observaciones
    ) {
        return ResponseEntity.ok(exhibicionObjetoService.verificarDevolucion(id, usuarioId, observaciones));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        exhibicionObjetoService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
