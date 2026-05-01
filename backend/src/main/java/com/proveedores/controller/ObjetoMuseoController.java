package com.proveedores.controller;

import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.service.ObjetoMuseoService;
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
@RequestMapping("/api/objetos")
public class ObjetoMuseoController {

    private final ObjetoMuseoService objetoMuseoService;

    public ObjetoMuseoController(ObjetoMuseoService objetoMuseoService) {
        this.objetoMuseoService = objetoMuseoService;
    }

    @PostMapping
    public ResponseEntity<ObjetoMuseoResponseDTO> crear(@RequestBody @Valid ObjetoMuseoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(objetoMuseoService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObjetoMuseoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(objetoMuseoService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<ObjetoMuseoResponseDTO>> listar() {
        return ResponseEntity.ok(objetoMuseoService.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ObjetoMuseoResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid ObjetoMuseoRequestDTO dto) {
        return ResponseEntity.ok(objetoMuseoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        objetoMuseoService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
