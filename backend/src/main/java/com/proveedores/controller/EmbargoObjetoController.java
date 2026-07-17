package com.proveedores.controller;

import com.proveedores.dto.EmbargoObjetoRequestDTO;
import com.proveedores.dto.EmbargoObjetoResponseDTO;
import com.proveedores.service.EmbargoObjetoService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Embargos de objetos", description = "Gestion administrativa de embargos de objetos")
@RestController
@RequestMapping("/api/admin/objetos/embargos")
public class EmbargoObjetoController {

    private final EmbargoObjetoService embargoObjetoService;

    public EmbargoObjetoController(EmbargoObjetoService embargoObjetoService) {
        this.embargoObjetoService = embargoObjetoService;
    }

    @Operation(summary = "Listar embargos")
    @ApiResponse(responseCode = "200", description = "Embargos obtenidos")
    @GetMapping
    public ResponseEntity<List<EmbargoObjetoResponseDTO>> listar(@RequestParam(defaultValue = "false") boolean incluirHistoricos) {
        return ResponseEntity.ok(embargoObjetoService.listar(incluirHistoricos));
    }

    @Operation(summary = "Registrar embargo")
    @ApiResponse(responseCode = "201", description = "Embargo registrado")
    @PostMapping
    public ResponseEntity<EmbargoObjetoResponseDTO> crear(@RequestBody @Valid EmbargoObjetoRequestDTO dto, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(embargoObjetoService.crear(dto, usuario(authentication)));
    }

    @Operation(summary = "Levantar embargo")
    @ApiResponse(responseCode = "200", description = "Embargo levantado")
    @PatchMapping("/{id}/levantar")
    public ResponseEntity<EmbargoObjetoResponseDTO> levantar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(embargoObjetoService.levantar(id, usuario(authentication)));
    }

    @Operation(summary = "Exportar embargos vigentes en PDF")
    @ApiResponse(responseCode = "200", description = "PDF generado")
    @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportarPdf(Authentication authentication) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivoPdf() + "\"")
                .body(embargoObjetoService.exportarVigentesPdf(usuario(authentication)));
    }

    private String nombreArchivoPdf() {
        return "embargos_objetos_" + DateTimeFormatter.ofPattern("yyyyMMdd_HHmm").format(LocalDateTime.now()) + ".pdf";
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
