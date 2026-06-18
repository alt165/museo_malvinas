package com.proveedores.service;

import com.proveedores.dto.FotoObjetoMuseoResponseDTO;
import com.proveedores.entity.FotoObjetoMuseo;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.FotoObjetoMuseoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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
    private final ObjectFileStorageService objectFileStorageService;
    private final long maxSizeBytes;

    public FotoObjetoMuseoService(
            FotoObjetoMuseoRepository fotoObjetoMuseoRepository,
            ObjetoMuseoService objetoMuseoService,
            ObjectFileStorageService objectFileStorageService,
            @org.springframework.beans.factory.annotation.Value("${app.upload.max-photo-size-mb}") long maxPhotoSizeMb
    ) {
        this.fotoObjetoMuseoRepository = fotoObjetoMuseoRepository;
        this.objetoMuseoService = objetoMuseoService;
        this.objectFileStorageService = objectFileStorageService;
        this.maxSizeBytes = maxPhotoSizeMb * 1024L * 1024L;
    }

    @Transactional
    public FotoObjetoMuseoResponseDTO subir(Long objetoId, MultipartFile archivo, String descripcion, String cargadoPor) {
        ObjetoMuseo objeto = objetoMuseoService.buscarObjetoActivo(objetoId);
        validarArchivo(archivo);

        ObjectFileStorageService.StoredObjectFile storedFile = objectFileStorageService.store(objetoId, "fotos", archivo);

        FotoObjetoMuseo foto = new FotoObjetoMuseo();
        foto.setObjetoMuseo(objeto);
        foto.setNombreArchivo(storedFile.originalName());
        foto.setNombreArchivoOriginal(storedFile.originalName());
        foto.setNombreArchivoAlmacenado(storedFile.storedName());
        foto.setContentType(archivo.getContentType());
        foto.setTamanioBytes(archivo.getSize());
        foto.setRutaAlmacenamiento(storedFile.absolutePath());
        foto.setRutaRelativa(storedFile.relativePath());
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
        Resource resource = StringUtils.hasText(foto.getRutaRelativa())
                ? objectFileStorageService.load(foto.getRutaRelativa())
                : new org.springframework.core.io.FileSystemResource(foto.getRutaAlmacenamiento());
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
                foto.getNombreArchivoAlmacenado(),
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
