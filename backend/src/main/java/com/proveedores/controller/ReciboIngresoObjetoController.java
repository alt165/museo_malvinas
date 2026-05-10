package com.proveedores.controller;

import com.proveedores.dto.ReciboIngresoObjetoResponseDTO;
import com.proveedores.service.ReciboIngresoObjetoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Recibos de ingreso", description = "Recibos emitidos por recepcion de objetos")
@RestController
@RequestMapping("/api/recibos")
public class ReciboIngresoObjetoController {

    private final ReciboIngresoObjetoService reciboIngresoObjetoService;

    public ReciboIngresoObjetoController(ReciboIngresoObjetoService reciboIngresoObjetoService) {
        this.reciboIngresoObjetoService = reciboIngresoObjetoService;
    }

    @Operation(summary = "Obtener recibo")
    @GetMapping("/{id}")
    public ResponseEntity<ReciboIngresoObjetoResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(reciboIngresoObjetoService.obtener(id));
    }

    @Operation(summary = "Descargar PDF de recibo")
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"recibo-" + id + ".pdf\"")
                .body(reciboIngresoObjetoService.generarPdf(id));
    }

    @Operation(summary = "Adjuntar copia firmada digitalizada del recibo")
    @PostMapping(path = "/{id}/copia-firmada", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReciboIngresoObjetoResponseDTO> subirCopiaFirmada(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo,
            Authentication authentication
    ) {
        return ResponseEntity.ok(reciboIngresoObjetoService.subirCopiaFirmada(id, archivo, usuario(authentication)));
    }

    @Operation(summary = "Descargar copia firmada digitalizada del recibo")
    @GetMapping("/{id}/copia-firmada")
    public ResponseEntity<Resource> descargarCopiaFirmada(@PathVariable Long id) {
        ReciboIngresoObjetoService.ReciboArchivo archivo = reciboIngresoObjetoService.descargarCopiaFirmada(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + archivo.nombreArchivo() + "\"")
                .body(archivo.resource());
    }

    private String usuario(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }
}
