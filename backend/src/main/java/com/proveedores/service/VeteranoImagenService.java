package com.proveedores.service;

import com.proveedores.dto.VeteranoImagenResponseDTO;
import com.proveedores.entity.Veterano;
import com.proveedores.entity.VeteranoImagen;
import com.proveedores.entity.TipoOperacionAuditoria;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.VeteranoImagenRepository;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VeteranoImagenService {

    private final VeteranoImagenRepository veteranoImagenRepository;
    private final VeteranoService veteranoService;
    private final ObjectFileStorageService objectFileStorageService;
    private final UploadFileValidator uploadFileValidator;
    private final TransactionalFileLifecycle transactionalFileLifecycle;
    private final AuditoriaObjetoService auditoriaService;
    private final long maxSizeBytes;
    private final int maxFilesPerRequest;

    public VeteranoImagenService(
            VeteranoImagenRepository veteranoImagenRepository,
            VeteranoService veteranoService,
            ObjectFileStorageService objectFileStorageService,
            UploadFileValidator uploadFileValidator,
            TransactionalFileLifecycle transactionalFileLifecycle,
            AuditoriaObjetoService auditoriaService,
            @org.springframework.beans.factory.annotation.Value("${app.upload.max-photo-size-mb}") long maxPhotoSizeMb,
            @org.springframework.beans.factory.annotation.Value("${app.upload.max-files-per-request:10}") int maxFilesPerRequest
    ) {
        this.veteranoImagenRepository = veteranoImagenRepository;
        this.veteranoService = veteranoService;
        this.objectFileStorageService = objectFileStorageService;
        this.uploadFileValidator = uploadFileValidator;
        this.transactionalFileLifecycle = transactionalFileLifecycle;
        this.auditoriaService = auditoriaService;
        this.maxSizeBytes = maxPhotoSizeMb * 1024L * 1024L;
        this.maxFilesPerRequest = maxFilesPerRequest;
    }

    @Transactional
    public VeteranoImagenResponseDTO subir(Long veteranoId, MultipartFile archivo, String descripcion, String cargadoPor) {
        Veterano veterano = veteranoService.buscarActivo(veteranoId);
        return guardar(veterano, validarArchivo(archivo), descripcion, cargadoPor);
    }

    @Transactional
    public List<VeteranoImagenResponseDTO> subirTodos(
            Long veteranoId,
            List<MultipartFile> archivos,
            String descripcion,
            String cargadoPor
    ) {
        if (archivos == null || archivos.isEmpty()) throw new BusinessException("Debe seleccionar al menos una imagen");
        if (archivos.size() > maxFilesPerRequest) {
            throw new BusinessException("La carga supera la cantidad maxima de archivos permitida");
        }
        Veterano veterano = veteranoService.buscarActivo(veteranoId);
        List<UploadFileValidator.ValidatedFile> validatedFiles = archivos.stream().map(this::validarArchivo).toList();
        return validatedFiles.stream().map(file -> guardar(veterano, file, descripcion, cargadoPor)).toList();
    }

    private VeteranoImagenResponseDTO guardar(
            Veterano veterano,
            UploadFileValidator.ValidatedFile file,
            String descripcion,
            String cargadoPor
    ) {
        ObjectFileStorageService.StoredObjectFile storedFile = objectFileStorageService.storeBytesInOwnerFolder(
                "veterano-" + veterano.getId(), "imagenes", file.bytes(), file.originalName(), file.extension());
        transactionalFileLifecycle.deleteOnRollback(() -> objectFileStorageService.delete(storedFile.relativePath()));

        VeteranoImagen imagen = new VeteranoImagen();
        imagen.setVeterano(veterano);
        imagen.setNombreArchivo(file.originalName());
        imagen.setNombreArchivoAlmacenado(storedFile.storedName());
        imagen.setTipoContenido(file.contentType());
        imagen.setTamanioBytes((long) file.bytes().length);
        imagen.setRutaArchivo(storedFile.absolutePath());
        imagen.setRutaRelativa(storedFile.relativePath());
        imagen.setDescripcion(descripcion);
        imagen.setOrden((int) veteranoImagenRepository.countByVeteranoIdAndEliminadoFalse(veterano.getId()));
        imagen.setFechaCarga(com.proveedores.time.MuseoTime.now());
        imagen.setCargadoPor(cargadoPor);
        VeteranoImagen saved = veteranoImagenRepository.save(imagen);
        auditoriaService.registrarEvento("VETERANO_IMAGEN", saved.getId(), null, TipoOperacionAuditoria.CREACION,
                "MULTIMEDIA_PERSONA_CARGADA", "Carga de imagen de veterano", "ARCHIVOS", null,
                auditoriaService.mapOf("veteranoId", veterano.getId(), "contentType", saved.getTipoContenido(),
                        "tamanioBytes", saved.getTamanioBytes()), cargadoPor);
        return toResponse(saved);
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
        imagen.setFechaEliminacion(com.proveedores.time.MuseoTime.now());
        veteranoImagenRepository.save(imagen);
        auditoriaService.registrarEvento("VETERANO_IMAGEN", imagen.getId(), null, TipoOperacionAuditoria.ELIMINACION,
                "MULTIMEDIA_PERSONA_ELIMINADA", "Baja lógica de imagen de veterano", "ARCHIVOS",
                auditoriaService.mapOf("veteranoId", veteranoId), null, null);
    }

    private VeteranoImagen buscarImagen(Long veteranoId, Long imagenId) {
        veteranoService.buscarActivo(veteranoId);
        return veteranoImagenRepository.findByIdAndVeteranoIdAndEliminadoFalse(imagenId, veteranoId)
                .orElseThrow(() -> new ResourceNotFoundException("Imagen del veterano no encontrada"));
    }

    private UploadFileValidator.ValidatedFile validarArchivo(MultipartFile archivo) {
        return uploadFileValidator.validateImage(archivo, maxSizeBytes, new UploadFileValidator.UploadMessages(
                "La imagen es obligatoria",
                "Tipo de imagen no permitido",
                "La imagen supera el tamano maximo permitido",
                "El archivo no contiene una imagen valida",
                "El contenido de la imagen no coincide con su tipo MIME",
                "No se pudo leer la imagen"
        ));
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
