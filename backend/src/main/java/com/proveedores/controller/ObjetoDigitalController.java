package com.proveedores.controller;

import com.proveedores.dto.ObjetoDigitalRequestDTO;
import com.proveedores.dto.ObjetoDigitalResponseDTO;
import com.proveedores.service.ObjetoDigitalService;
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
@RequestMapping("/api/objetos-digitales")
public class ObjetoDigitalController {

    private final ObjetoDigitalService objetoDigitalService;

    public ObjetoDigitalController(ObjetoDigitalService objetoDigitalService) {
        this.objetoDigitalService = objetoDigitalService;
    }

    @PostMapping
    public ResponseEntity<ObjetoDigitalResponseDTO> crear(@RequestBody @Valid ObjetoDigitalRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(objetoDigitalService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObjetoDigitalResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(objetoDigitalService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<ObjetoDigitalResponseDTO>> listar() {
        return ResponseEntity.ok(objetoDigitalService.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ObjetoDigitalResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid ObjetoDigitalRequestDTO dto) {
        return ResponseEntity.ok(objetoDigitalService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        objetoDigitalService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}
