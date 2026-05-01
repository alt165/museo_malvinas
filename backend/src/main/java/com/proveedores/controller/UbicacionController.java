package com.proveedores.controller;

import com.proveedores.dto.UbicacionRequestDTO;
import com.proveedores.dto.UbicacionResponseDTO;
import com.proveedores.service.UbicacionService;
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
@RequestMapping("/api/ubicaciones")
public class UbicacionController {

    private final UbicacionService ubicacionService;

    public UbicacionController(UbicacionService ubicacionService) {
        this.ubicacionService = ubicacionService;
    }

    @PostMapping
    public ResponseEntity<UbicacionResponseDTO> crear(@RequestBody @Valid UbicacionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ubicacionService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UbicacionResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ubicacionService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<UbicacionResponseDTO>> listar() {
        return ResponseEntity.ok(ubicacionService.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UbicacionResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid UbicacionRequestDTO dto) {
        return ResponseEntity.ok(ubicacionService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        ubicacionService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
