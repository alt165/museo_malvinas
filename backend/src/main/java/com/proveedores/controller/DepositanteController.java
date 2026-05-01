package com.proveedores.controller;

import com.proveedores.dto.DepositanteRequestDTO;
import com.proveedores.dto.DepositanteResponseDTO;
import com.proveedores.service.DepositanteService;
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
@RequestMapping("/api/depositantes")
public class DepositanteController {

    private final DepositanteService depositanteService;

    public DepositanteController(DepositanteService depositanteService) {
        this.depositanteService = depositanteService;
    }

    @PostMapping
    public ResponseEntity<DepositanteResponseDTO> crear(@RequestBody @Valid DepositanteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(depositanteService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepositanteResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(depositanteService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<DepositanteResponseDTO>> listar() {
        return ResponseEntity.ok(depositanteService.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepositanteResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid DepositanteRequestDTO dto) {
        return ResponseEntity.ok(depositanteService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        depositanteService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
