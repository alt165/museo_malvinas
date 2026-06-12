package com.proveedores.controller;

import com.proveedores.dto.DepositanteRequestDTO;
import com.proveedores.dto.DepositanteResponseDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.service.DepositanteExportService;
import com.proveedores.service.DepositanteService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Depositantes", description = "Personas o instituciones depositantes de objetos")
@RestController
@RequestMapping("/api/depositantes")
public class DepositanteController {

    private final DepositanteService depositanteService;
    private final DepositanteExportService depositanteExportService;

    public DepositanteController(DepositanteService depositanteService, DepositanteExportService depositanteExportService) {
        this.depositanteService = depositanteService;
        this.depositanteExportService = depositanteExportService;
    }

    @Operation(summary = "Crear recurso")
    @ApiResponse(responseCode = "201", description = "Recurso creado")
    @PostMapping
    public ResponseEntity<DepositanteResponseDTO> crear(@RequestBody @Valid DepositanteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(depositanteService.crear(dto));
    }

    @Operation(summary = "Obtener recurso por id")
    @ApiResponse(responseCode = "200", description = "Recurso encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<DepositanteResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(depositanteService.obtenerPorId(id));
    }

    @Operation(summary = "Listar recursos")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<DepositanteResponseDTO>> listar() {
        return ResponseEntity.ok(depositanteService.listar());
    }

    @Operation(summary = "Buscar depositante por DNI o CUIT")
    @ApiResponse(responseCode = "200", description = "Depositante encontrado")
    @ApiResponse(responseCode = "404", description = "Depositante no encontrado")
    @GetMapping("/buscar-identificacion")
    public ResponseEntity<DepositanteResponseDTO> buscarPorIdentificacion(@RequestParam String valor) {
        return ResponseEntity.ok(depositanteService.buscarPorIdentificacion(valor));
    }

    @Operation(summary = "Buscar depositantes por nombre")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping("/buscar-nombre")
    public ResponseEntity<List<DepositanteResponseDTO>> buscarPorNombre(@RequestParam String valor) {
        return ResponseEntity.ok(depositanteService.buscarPorNombre(valor));
    }

    @Operation(summary = "Listar objetos entregados por un depositante")
    @ApiResponse(responseCode = "200", description = "Objetos obtenidos")
    @GetMapping("/{id}/objetos")
    public ResponseEntity<List<ObjetoMuseoResponseDTO>> listarObjetos(@PathVariable Long id) {
        return ResponseEntity.ok(depositanteService.listarObjetos(id));
    }

    @Operation(summary = "Exportar reporte PDF de objetos entregados por un depositante")
    @ApiResponse(responseCode = "200", description = "PDF generado")
    @GetMapping(value = "/{id}/objetos/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportarObjetosPdf(@PathVariable Long id, Authentication authentication) {
        byte[] pdf = depositanteExportService.exportarObjetosPdf(id, usuario(authentication));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivoDepositanteObjetosPdf(id) + "\"")
                .body(pdf);
    }

    @Operation(summary = "Actualizar recurso")
    @ApiResponse(responseCode = "200", description = "Recurso actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<DepositanteResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid DepositanteRequestDTO dto) {
        return ResponseEntity.ok(depositanteService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja recurso")
    @ApiResponse(responseCode = "204", description = "Recurso dado de baja")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        depositanteService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }

    private String nombreArchivoDepositanteObjetosPdf(Long id) {
        return "depositante_" + id + "_objetos_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")) + ".pdf";
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
