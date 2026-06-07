package com.proveedores.controller;

import com.proveedores.dto.ActualizarFechaVencimientoRequestDTO;
import com.proveedores.dto.ComodatoPrestamoResponseDTO;
import com.proveedores.dto.ConfigAlertasVencimientoDTO;
import com.proveedores.service.ComodatoPrestamoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Comodatos y prestamos", description = "Administracion de objetos recibidos como comodato o prestamo")
@RestController
@RequestMapping("/api/admin/comodatos-prestamos")
public class ComodatoPrestamoAdminController {

    private final ComodatoPrestamoService comodatoPrestamoService;

    public ComodatoPrestamoAdminController(ComodatoPrestamoService comodatoPrestamoService) {
        this.comodatoPrestamoService = comodatoPrestamoService;
    }

    @Operation(summary = "Listar comodatos y prestamos")
    @GetMapping
    public ResponseEntity<List<ComodatoPrestamoResponseDTO>> listar() {
        return ResponseEntity.ok(comodatoPrestamoService.listar());
    }

    @Operation(summary = "Actualizar fecha de vencimiento")
    @PatchMapping("/{objetoId}/fecha-vencimiento")
    public ResponseEntity<ComodatoPrestamoResponseDTO> actualizarFechaVencimiento(
            @PathVariable Long objetoId,
            @RequestBody @Valid ActualizarFechaVencimientoRequestDTO dto
    ) {
        return ResponseEntity.ok(comodatoPrestamoService.actualizarFechaVencimiento(objetoId, dto.fechaVencimiento()));
    }

    @Operation(summary = "Obtener configuracion de alertas")
    @GetMapping("/config-alertas")
    public ResponseEntity<ConfigAlertasVencimientoDTO> obtenerConfigAlertas() {
        return ResponseEntity.ok(comodatoPrestamoService.obtenerConfigAlertas());
    }

    @Operation(summary = "Actualizar configuracion de alertas")
    @PutMapping("/config-alertas")
    public ResponseEntity<ConfigAlertasVencimientoDTO> actualizarConfigAlertas(@RequestBody @Valid ConfigAlertasVencimientoDTO dto) {
        return ResponseEntity.ok(comodatoPrestamoService.actualizarConfigAlertas(dto));
    }
}
