package com.proveedores.controller;

import com.proveedores.dto.VeteranoImagenResponseDTO;
import com.proveedores.dto.VeteranoRequestDTO;
import com.proveedores.dto.VeteranoResponseDTO;
import com.proveedores.dto.VeteranoVideoRequestDTO;
import com.proveedores.dto.VeteranoVideoResponseDTO;
import com.proveedores.service.VeteranoImagenService;
import com.proveedores.service.VeteranoService;
import com.proveedores.service.VeteranoVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.Resource;
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
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Veteranos", description = "Gestion de veteranos relacionados al museo")
@RestController
@RequestMapping("/api/veteranos")
public class VeteranoController {

    private final VeteranoService veteranoService;
    private final VeteranoImagenService veteranoImagenService;
    private final VeteranoVideoService veteranoVideoService;

    public VeteranoController(
            VeteranoService veteranoService,
            VeteranoImagenService veteranoImagenService,
            VeteranoVideoService veteranoVideoService
    ) {
        this.veteranoService = veteranoService;
        this.veteranoImagenService = veteranoImagenService;
        this.veteranoVideoService = veteranoVideoService;
    }

    @Operation(summary = "Crear recurso")
    @ApiResponse(responseCode = "201", description = "Recurso creado")
    @PostMapping
    public ResponseEntity<VeteranoResponseDTO> crear(@RequestBody @Valid VeteranoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(veteranoService.crear(dto));
    }

    @Operation(summary = "Obtener recurso por id")
    @ApiResponse(responseCode = "200", description = "Recurso encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<VeteranoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(veteranoService.obtenerPorId(id));
    }

    @Operation(summary = "Listar recursos")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<VeteranoResponseDTO>> listar() {
        return ResponseEntity.ok(veteranoService.listar());
    }

    @Operation(summary = "Actualizar recurso")
    @ApiResponse(responseCode = "200", description = "Recurso actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<VeteranoResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid VeteranoRequestDTO dto) {
        return ResponseEntity.ok(veteranoService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja recurso")
    @ApiResponse(responseCode = "204", description = "Recurso dado de baja")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        veteranoService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Subir imagenes del veterano")
    @PostMapping(path = "/{id}/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<VeteranoImagenResponseDTO>> subirImagenes(
            @PathVariable Long id,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            Authentication authentication
    ) {
        List<MultipartFile> archivosParaSubir = new ArrayList<>();
        if (archivos != null) {
            archivosParaSubir.addAll(archivos);
        }
        if (archivo != null) {
            archivosParaSubir.add(archivo);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(archivosParaSubir.stream()
                        .map(item -> veteranoImagenService.subir(id, item, descripcion, usuario(authentication)))
                        .toList());
    }

    @Operation(summary = "Listar imagenes del veterano")
    @GetMapping("/{id}/imagenes")
    public ResponseEntity<List<VeteranoImagenResponseDTO>> listarImagenes(@PathVariable Long id) {
        return ResponseEntity.ok(veteranoImagenService.listar(id));
    }

    @Operation(summary = "Descargar imagen del veterano")
    @GetMapping("/{id}/imagenes/{imagenId}")
    public ResponseEntity<Resource> descargarImagen(@PathVariable Long id, @PathVariable Long imagenId) {
        VeteranoImagenService.ImagenArchivo imagen = veteranoImagenService.descargar(id, imagenId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.metadata().tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + imagen.metadata().nombreArchivo() + "\"")
                .body(imagen.resource());
    }

    @Operation(summary = "Eliminar imagen del veterano")
    @DeleteMapping("/{id}/imagenes/{imagenId}")
    public ResponseEntity<Void> eliminarImagen(@PathVariable Long id, @PathVariable Long imagenId) {
        veteranoImagenService.eliminar(id, imagenId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Crear video del veterano")
    @PostMapping("/{id}/videos")
    public ResponseEntity<VeteranoVideoResponseDTO> crearVideo(@PathVariable Long id, @RequestBody @Valid VeteranoVideoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(veteranoVideoService.crear(id, dto));
    }

    @Operation(summary = "Listar videos del veterano")
    @GetMapping("/{id}/videos")
    public ResponseEntity<List<VeteranoVideoResponseDTO>> listarVideos(@PathVariable Long id) {
        return ResponseEntity.ok(veteranoVideoService.listar(id));
    }

    @Operation(summary = "Actualizar video del veterano")
    @PutMapping("/{id}/videos/{videoId}")
    public ResponseEntity<VeteranoVideoResponseDTO> actualizarVideo(
            @PathVariable Long id,
            @PathVariable Long videoId,
            @RequestBody @Valid VeteranoVideoRequestDTO dto
    ) {
        return ResponseEntity.ok(veteranoVideoService.actualizar(id, videoId, dto));
    }

    @Operation(summary = "Eliminar video del veterano")
    @DeleteMapping("/{id}/videos/{videoId}")
    public ResponseEntity<Void> eliminarVideo(@PathVariable Long id, @PathVariable Long videoId) {
        veteranoVideoService.eliminar(id, videoId);
        return ResponseEntity.noContent().build();
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

