package com.proveedores.controller;

import com.proveedores.dto.AgregarCategoriaObjetoRequestDTO;
import com.proveedores.dto.CargaRapidaObjetoRequestDTO;
import com.proveedores.dto.CargaRapidaObjetoResponseDTO;
import com.proveedores.dto.FotoObjetoMuseoResponseDTO;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.dto.ReciboIngresoObjetoResponseDTO;
import com.proveedores.service.FotoObjetoMuseoService;
import com.proveedores.service.ObjetoMuseoService;
import com.proveedores.service.ReciboIngresoObjetoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

@Tag(name = "Objetos de museo", description = "Gestion de objetos patrimoniales del museo")
@RestController
@RequestMapping("/api/objetos")
public class ObjetoMuseoController {

    private final ObjetoMuseoService objetoMuseoService;
    private final FotoObjetoMuseoService fotoObjetoMuseoService;
    private final ReciboIngresoObjetoService reciboIngresoObjetoService;

    public ObjetoMuseoController(
            ObjetoMuseoService objetoMuseoService,
            FotoObjetoMuseoService fotoObjetoMuseoService,
            ReciboIngresoObjetoService reciboIngresoObjetoService
    ) {
        this.objetoMuseoService = objetoMuseoService;
        this.fotoObjetoMuseoService = fotoObjetoMuseoService;
        this.reciboIngresoObjetoService = reciboIngresoObjetoService;
    }

    @Operation(summary = "Crear recurso")
    @ApiResponse(responseCode = "201", description = "Recurso creado")
    @PostMapping
    public ResponseEntity<ObjetoMuseoResponseDTO> crear(@RequestBody @Valid ObjetoMuseoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(objetoMuseoService.crear(dto));
    }

    @Operation(summary = "Obtener recurso por id")
    @ApiResponse(responseCode = "200", description = "Recurso encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<ObjetoMuseoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(objetoMuseoService.obtenerPorId(id));
    }

    @Operation(summary = "Listar recursos")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    @GetMapping
    public ResponseEntity<List<ObjetoMuseoResponseDTO>> listar() {
        return ResponseEntity.ok(objetoMuseoService.listar());
    }

    @Operation(
            summary = "Buscar objetos de museo con paginacion",
            description = "Permite ordenar por numeroInventario, denominacionObjeto, descripcion, descripcionTecnica, estadoConservacion y fechaIngreso. El ordenamiento por categorias no se aplica por tratarse de una relacion multiple."
    )
    @ApiResponse(responseCode = "200", description = "Busqueda obtenida")
    @GetMapping("/buscar")
    public ResponseEntity<Page<ObjetoMuseoResponseDTO>> buscar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String numeroInventario,
            @RequestParam(required = false) List<Long> categoriaIds,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(objetoMuseoService.buscar(nombre, numeroInventario, categoriaIds, pageable));
    }

    @Operation(summary = "Actualizar recurso")
    @ApiResponse(responseCode = "200", description = "Recurso actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<ObjetoMuseoResponseDTO> actualizar(@PathVariable Long id, @RequestBody @Valid ObjetoMuseoRequestDTO dto) {
        return ResponseEntity.ok(objetoMuseoService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja recurso")
    @ApiResponse(responseCode = "204", description = "Recurso dado de baja")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id, Authentication authentication) {
        objetoMuseoService.bajaLogica(id, usuario(authentication));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Agregar categoria al objeto")
    @PostMapping("/{id}/categorias")
    public ResponseEntity<ObjetoMuseoResponseDTO> agregarCategoria(
            @PathVariable Long id,
            @RequestBody @Valid AgregarCategoriaObjetoRequestDTO dto
    ) {
        return ResponseEntity.ok(objetoMuseoService.agregarCategoria(id, dto));
    }

    @Operation(summary = "Quitar categoria del objeto")
    @DeleteMapping("/{id}/categorias/{categoriaId}")
    public ResponseEntity<ObjetoMuseoResponseDTO> quitarCategoria(@PathVariable Long id, @PathVariable Long categoriaId) {
        return ResponseEntity.ok(objetoMuseoService.quitarCategoria(id, categoriaId));
    }

    @Operation(summary = "Subir foto del objeto")
    @PostMapping(path = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FotoObjetoMuseoResponseDTO> subirFoto(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fotoObjetoMuseoService.subir(id, archivo, descripcion, usuario(authentication)));
    }

    @Operation(summary = "Listar fotos del objeto")
    @GetMapping("/{id}/fotos")
    public ResponseEntity<List<FotoObjetoMuseoResponseDTO>> listarFotos(@PathVariable Long id) {
        return ResponseEntity.ok(fotoObjetoMuseoService.listar(id));
    }

    @Operation(summary = "Listar recibos emitidos para el objeto")
    @GetMapping("/{id}/recibos")
    public ResponseEntity<List<ReciboIngresoObjetoResponseDTO>> listarRecibos(@PathVariable Long id) {
        objetoMuseoService.obtenerPorId(id);
        return ResponseEntity.ok(reciboIngresoObjetoService.listarPorObjeto(id));
    }

    @Operation(summary = "Descargar foto del objeto")
    @GetMapping("/{id}/fotos/{fotoId}")
    public ResponseEntity<Resource> descargarFoto(@PathVariable Long id, @PathVariable Long fotoId) {
        FotoObjetoMuseoService.FotoArchivo foto = fotoObjetoMuseoService.descargar(id, fotoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.metadata().contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + foto.metadata().nombreArchivo() + "\"")
                .body(foto.resource());
    }

    @Operation(summary = "Eliminar foto del objeto")
    @DeleteMapping("/{id}/fotos/{fotoId}")
    public ResponseEntity<Void> eliminarFoto(@PathVariable Long id, @PathVariable Long fotoId) {
        fotoObjetoMuseoService.eliminar(id, fotoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Crear objeto por carga rapida y emitir recibo")
    @PostMapping("/carga-rapida")
    public ResponseEntity<CargaRapidaObjetoResponseDTO> cargaRapida(
            @RequestBody @Valid CargaRapidaObjetoRequestDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(objetoMuseoService.cargaRapida(dto, usuario(authentication)));
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
