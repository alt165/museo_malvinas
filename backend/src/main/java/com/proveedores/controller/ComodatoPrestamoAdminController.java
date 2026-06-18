package com.proveedores.controller;

import com.proveedores.dto.ActualizarFechaVencimientoRequestDTO;
import com.proveedores.dto.ComodatoPrestamoResponseDTO;
import com.proveedores.dto.ConfigAlertasVencimientoDTO;
import com.proveedores.service.ComodatoPrestamoExportService;
import com.proveedores.service.ComodatoPrestamoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
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
    private final ComodatoPrestamoExportService comodatoPrestamoExportService;

    public ComodatoPrestamoAdminController(ComodatoPrestamoService comodatoPrestamoService, ComodatoPrestamoExportService comodatoPrestamoExportService) {
        this.comodatoPrestamoService = comodatoPrestamoService;
        this.comodatoPrestamoExportService = comodatoPrestamoExportService;
    }

    @Operation(summary = "Listar comodatos y prestamos")
    @GetMapping
    public ResponseEntity<List<ComodatoPrestamoResponseDTO>> listar() {
        return ResponseEntity.ok(comodatoPrestamoService.listar());
    }

    @Operation(summary = "Exportar comodatos y prestamos en PDF")
    @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportarPdf(Authentication authentication) {
        byte[] pdf = comodatoPrestamoExportService.exportarPdf(usuario(authentication));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivoPdf() + "\"")
                .body(pdf);
    }

    @Operation(summary = "Actualizar fecha de vencimiento")
    @PatchMapping("/{objetoId}/fecha-vencimiento")
    public ResponseEntity<ComodatoPrestamoResponseDTO> actualizarFechaVencimiento(
            @PathVariable Long objetoId,
            @RequestBody @Valid ActualizarFechaVencimientoRequestDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(comodatoPrestamoService.actualizarFechaVencimiento(objetoId, dto.fechaVencimiento(), usuario(authentication)));
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

    private String nombreArchivoPdf() {
        return "comodatos_prestamos_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")) + ".pdf";
    }

    private String usuario(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            String username = jwtAuthentication.getToken().getClaimAsString("preferred_username");
            if (StringUtils.hasText(username)) {
                return username;
            }
        }
        return authentication.getName();
    }
}
