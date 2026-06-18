package com.proveedores.service;

import com.proveedores.dto.VeteranoImagenResponseDTO;
import com.proveedores.entity.Veterano;
import com.proveedores.entity.VeteranoImagen;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.VeteranoImagenRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VeteranoImagenService {

    private static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");

    private final VeteranoImagenRepository veteranoImagenRepository;
    private final VeteranoService veteranoService;
    private final ObjectFileStorageService objectFileStorageService;
    private final long maxSizeBytes;

    public VeteranoImagenService(
            VeteranoImagenRepository veteranoImagenRepository,
            VeteranoService veteranoService,
            ObjectFileStorageService objectFileStorageService,
            @org.springframework.beans.factory.annotation.Value("${app.upload.max-photo-size-mb}") long maxPhotoSizeMb
    ) {
        this.veteranoImagenRepository = veteranoImagenRepository;
        this.veteranoService = veteranoService;
        this.objectFileStorageService = objectFileStorageService;
        this.maxSizeBytes = maxPhotoSizeMb * 1024L * 1024L;
    }

    @Transactional
    public VeteranoImagenResponseDTO subir(Long veteranoId, MultipartFile archivo, String descripcion, String cargadoPor) {
        Veterano veterano = veteranoService.buscarActivo(veteranoId);
        validarArchivo(archivo);

        ObjectFileStorageService.StoredObjectFile storedFile = objectFileStorageService.storeInOwnerFolder("veterano-" + veteranoId, "imagenes", archivo);

        VeteranoImagen imagen = new VeteranoImagen();
        imagen.setVeterano(veterano);
        imagen.setNombreArchivo(storedFile.originalName());
        imagen.setNombreArchivoAlmacenado(storedFile.storedName());
        imagen.setTipoContenido(archivo.getContentType());
        imagen.setTamanioBytes(archivo.getSize());
        imagen.setRutaArchivo(storedFile.absolutePath());
        imagen.setRutaRelativa(storedFile.relativePath());
        imagen.setDescripcion(descripcion);
        imagen.setOrden((int) veteranoImagenRepository.countByVeteranoIdAndEliminadoFalse(veteranoId));
        imagen.setFechaCarga(LocalDateTime.now());
        imagen.setCargadoPor(cargadoPor);
        return toResponse(veteranoImagenRepository.save(imagen));
    }

    @Transactional(readOnly = true)
    public List<VeteranoImagenResponseDTO> listar(Long veteranoId) {
        veteranoService.buscarActivo(veteranoId);
        return veteranoImagenRepository.findByVeteranoIdAndEliminadoFalseOrderByOrdenAscFechaCargaAscIdAsc(veteranoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ImagenArchivo descargar(Long veteranoId, Long imagenId) {
        VeteranoImagen imagen = buscarImagen(veteranoId, imagenId);
        Resource resource = StringUtils.hasText(imagen.getRutaRelativa())
                ? objectFileStorageService.load(imagen.getRutaRelativa())
                : new org.springframework.core.io.FileSystemResource(imagen.getRutaArchivo());
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Archivo de imagen no encontrado");
        }
        return new ImagenArchivo(toResponse(imagen), resource);
    }

    @Transactional
    public void eliminar(Long veteranoId, Long imagenId) {
        VeteranoImagen imagen = buscarImagen(veteranoId, imagenId);
        imagen.setActivo(false);
        imagen.setEliminado(true);
        imagen.setFechaEliminacion(LocalDateTime.now());
        veteranoImagenRepository.save(imagen);
    }

    private VeteranoImagen buscarImagen(Long veteranoId, Long imagenId) {
        veteranoService.buscarActivo(veteranoId);
        return veteranoImagenRepository.findByIdAndVeteranoIdAndEliminadoFalse(imagenId, veteranoId)
                .orElseThrow(() -> new ResourceNotFoundException("Imagen del veterano no encontrada"));
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("La imagen es obligatoria");
        }
        if (!CONTENT_TYPES_PERMITIDOS.contains(archivo.getContentType())) {
            throw new BusinessException("Tipo de imagen no permitido");
        }
        if (archivo.getSize() > maxSizeBytes) {
            throw new BusinessException("La imagen supera el tamano maximo permitido");
        }
    }

    private VeteranoImagenResponseDTO toResponse(VeteranoImagen imagen) {
        return new VeteranoImagenResponseDTO(
                imagen.getId(),
                imagen.getVeterano().getId(),
                imagen.getNombreArchivo(),
                imagen.getNombreArchivoAlmacenado(),
                imagen.getTipoContenido(),
                imagen.getTamanioBytes(),
                imagen.getDescripcion(),
                imagen.getOrden(),
                imagen.getFechaCarga(),
                imagen.getCargadoPor()
        );
    }

    public record ImagenArchivo(VeteranoImagenResponseDTO metadata, Resource resource) {
    }
}
