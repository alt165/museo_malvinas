package com.proveedores.controller;

import com.proveedores.dto.VeteranoRequestDTO;
import com.proveedores.dto.VeteranoResponseDTO;
import com.proveedores.service.VeteranoService;
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
@RequestMapping("/api/veteranos")
public class VeteranoController {

    private final VeteranoService veteranoService;

    public VeteranoController(VeteranoService veteranoService) {
        this.veteranoService = veteranoService;
    }

    @PostMapping
    public ResponseEntity<VeteranoResponseDTO> crear(@RequestBody @Valid VeteranoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(veteranoService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VeteranoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(veteranoService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<VeteranoResponseDTO>> listar() {
        return ResponseEntity.ok(veteranoService.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<VeteranoResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid VeteranoRequestDTO dto) {
        return ResponseEntity.ok(veteranoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        veteranoService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
