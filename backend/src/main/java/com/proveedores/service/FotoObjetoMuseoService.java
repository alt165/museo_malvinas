package com.proveedores.service;

import com.proveedores.dto.FotoObjetoMuseoResponseDTO;
import com.proveedores.entity.FotoObjetoMuseo;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.FotoObjetoMuseoRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FotoObjetoMuseoService {

    private static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");

    private final FotoObjetoMuseoRepository fotoObjetoMuseoRepository;
    private final ObjetoMuseoService objetoMuseoService;
    private final Path storageDir;
    private final long maxSizeBytes;

    public FotoObjetoMuseoService(
            FotoObjetoMuseoRepository fotoObjetoMuseoRepository,
            ObjetoMuseoService objetoMuseoService,
            @Value("${app.storage.object-photos-dir}") String storageDir,
            @Value("${app.upload.max-photo-size-mb}") long maxPhotoSizeMb
    ) {
        this.fotoObjetoMuseoRepository = fotoObjetoMuseoRepository;
        this.objetoMuseoService = objetoMuseoService;
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
        this.maxSizeBytes = maxPhotoSizeMb * 1024L * 1024L;
    }

    @Transactional
    public FotoObjetoMuseoResponseDTO subir(Long objetoId, MultipartFile archivo, String descripcion, String cargadoPor) {
        ObjetoMuseo objeto = objetoMuseoService.buscarObjetoActivo(objetoId);
        validarArchivo(archivo);

        String nombreOriginal = StringUtils.hasText(archivo.getOriginalFilename()) ? archivo.getOriginalFilename() : "foto";
        String nombreSeguro = nombreOriginal.replaceAll("[^A-Za-z0-9._-]", "_");
        Path directorioObjeto = storageDir.resolve(String.valueOf(objetoId)).normalize();
        Path destino = directorioObjeto.resolve(UUID.randomUUID() + "-" + nombreSeguro).normalize();
        if (!destino.startsWith(storageDir)) {
            throw new BusinessException("Nombre de archivo invalido");
        }

        try {
            Files.createDirectories(directorioObjeto);
            archivo.transferTo(destino);
        } catch (IOException ex) {
            throw new BusinessException("No se pudo almacenar la foto del objeto");
        }

        FotoObjetoMuseo foto = new FotoObjetoMuseo();
        foto.setObjetoMuseo(objeto);
        foto.setNombreArchivo(nombreOriginal);
        foto.setContentType(archivo.getContentType());
        foto.setTamanioBytes(archivo.getSize());
        foto.setRutaAlmacenamiento(destino.toString());
        foto.setDescripcion(descripcion);
        foto.setFechaCarga(LocalDateTime.now());
        foto.setCargadoPor(cargadoPor);
        return toResponse(fotoObjetoMuseoRepository.save(foto));
    }

    @Transactional(readOnly = true)
    public List<FotoObjetoMuseoResponseDTO> listar(Long objetoId) {
        objetoMuseoService.buscarObjetoActivo(objetoId);
        return fotoObjetoMuseoRepository.findByObjetoMuseoIdAndEliminadoFalse(objetoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FotoArchivo descargar(Long objetoId, Long fotoId) {
        FotoObjetoMuseo foto = buscarFoto(objetoId, fotoId);
        Resource resource = new FileSystemResource(foto.getRutaAlmacenamiento());
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Archivo de foto no encontrado");
        }
        return new FotoArchivo(toResponse(foto), resource);
    }

    @Transactional
    public void eliminar(Long objetoId, Long fotoId) {
        FotoObjetoMuseo foto = buscarFoto(objetoId, fotoId);
        foto.setActivo(false);
        foto.setEliminado(true);
        foto.setFechaEliminacion(LocalDateTime.now());
        fotoObjetoMuseoRepository.save(foto);
    }

    private FotoObjetoMuseo buscarFoto(Long objetoId, Long fotoId) {
        objetoMuseoService.buscarObjetoActivo(objetoId);
        return fotoObjetoMuseoRepository.findByIdAndObjetoMuseoIdAndEliminadoFalse(fotoId, objetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Foto del objeto no encontrada"));
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("La foto es obligatoria");
        }
        if (!CONTENT_TYPES_PERMITIDOS.contains(archivo.getContentType())) {
            throw new BusinessException("Tipo de imagen no permitido");
        }
        if (archivo.getSize() > maxSizeBytes) {
            throw new BusinessException("La foto supera el tamano maximo permitido");
        }
    }

    private FotoObjetoMuseoResponseDTO toResponse(FotoObjetoMuseo foto) {
        return new FotoObjetoMuseoResponseDTO(
                foto.getId(),
                foto.getObjetoMuseo().getId(),
                foto.getNombreArchivo(),
                foto.getContentType(),
                foto.getTamanioBytes(),
                foto.getDescripcion(),
                foto.getFechaCarga(),
                foto.getCargadoPor()
        );
    }

    public record FotoArchivo(FotoObjetoMuseoResponseDTO metadata, Resource resource) {
    }
}
