package com.proveedores.controller;

import com.proveedores.dto.AgregarObjetosColeccionRequestDTO;
import com.proveedores.dto.ColeccionObjetoRequestDTO;
import com.proveedores.dto.ColeccionObjetoResponseDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.service.ColeccionObjetoExportService;
import com.proveedores.service.ColeccionObjetoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Colecciones de objetos", description = "Gestion de colecciones de objetos patrimoniales")
@RestController
@RequestMapping("/api/colecciones")
public class ColeccionObjetoController {

    private final ColeccionObjetoService coleccionObjetoService;
    private final ColeccionObjetoExportService coleccionObjetoExportService;

    public ColeccionObjetoController(ColeccionObjetoService coleccionObjetoService, ColeccionObjetoExportService coleccionObjetoExportService) {
        this.coleccionObjetoService = coleccionObjetoService;
        this.coleccionObjetoExportService = coleccionObjetoExportService;
    }

    @Operation(summary = "Crear coleccion")
    @ApiResponse(responseCode = "201", description = "Coleccion creada")
    @PostMapping
    public ResponseEntity<ColeccionObjetoResponseDTO> crear(@RequestBody @Valid ColeccionObjetoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(coleccionObjetoService.crear(dto));
    }

    @Operation(summary = "Listar colecciones")
    @GetMapping
    public ResponseEntity<List<ColeccionObjetoResponseDTO>> listar() {
        return ResponseEntity.ok(coleccionObjetoService.listar());
    }

    @Operation(summary = "Obtener coleccion por id")
    @GetMapping("/{id}")
    public ResponseEntity<ColeccionObjetoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(coleccionObjetoService.obtenerPorId(id));
    }

    @Operation(summary = "Actualizar coleccion")
    @PutMapping("/{id}")
    public ResponseEntity<ColeccionObjetoResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid ColeccionObjetoRequestDTO dto) {
        return ResponseEntity.ok(coleccionObjetoService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja coleccion")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        coleccionObjetoService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar objetos de una coleccion")
    @GetMapping("/{id}/objetos")
    public ResponseEntity<List<ObjetoMuseoResponseDTO>> listarObjetos(@PathVariable Long id) {
        return ResponseEntity.ok(coleccionObjetoService.listarObjetos(id));
    }

    @Operation(summary = "Exportar reporte de una coleccion en PDF")
    @ApiResponse(responseCode = "200", description = "PDF generado")
    @GetMapping(value = "/{id}/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportarPdf(@PathVariable Long id, Authentication authentication) {
        byte[] pdf = coleccionObjetoExportService.exportarPdf(id, usuario(authentication));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivoColeccionPdf(id) + "\"")
                .body(pdf);
    }

    @Operation(summary = "Agregar objetos a una coleccion")
    @PostMapping("/{id}/objetos")
    public ResponseEntity<List<ObjetoMuseoResponseDTO>> agregarObjetos(
            @PathVariable Long id,
            @RequestBody @Valid AgregarObjetosColeccionRequestDTO dto
    ) {
        return ResponseEntity.ok(coleccionObjetoService.agregarObjetos(id, dto));
    }

    @Operation(summary = "Quitar objeto de una coleccion")
    @DeleteMapping("/{id}/objetos/{objetoId}")
    public ResponseEntity<Void> quitarObjeto(@PathVariable Long id, @PathVariable Long objetoId) {
        coleccionObjetoService.quitarObjeto(id, objetoId);
        return ResponseEntity.noContent().build();
    }

    private String nombreArchivoColeccionPdf(Long id) {
        return "coleccion_" + id + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")) + ".pdf";
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
