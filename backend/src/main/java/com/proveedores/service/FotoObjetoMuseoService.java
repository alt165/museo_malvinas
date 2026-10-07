package com.proveedores.service;

import com.proveedores.dto.FotoObjetoMuseoResponseDTO;
import com.proveedores.dto.RegeneracionFotosPublicasResponseDTO;
import com.proveedores.entity.FotoObjetoMuseo;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.VisibilidadCampo;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.FotoObjetoMuseoRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FotoObjetoMuseoService {

    private final FotoObjetoMuseoRepository fotoObjetoMuseoRepository;
    private final ObjetoMuseoService objetoMuseoService;
    private final ObjectFileStorageService objectFileStorageService;
    private final ImageWatermarkService imageWatermarkService;
    private final UploadFileValidator uploadFileValidator;
    private final TransactionalFileLifecycle transactionalFileLifecycle;
    private final long maxSizeBytes;
    private final int maxFilesPerRequest;

    public FotoObjetoMuseoService(
            FotoObjetoMuseoRepository fotoObjetoMuseoRepository,
            ObjetoMuseoService objetoMuseoService,
            ObjectFileStorageService objectFileStorageService,
            ImageWatermarkService imageWatermarkService,
            UploadFileValidator uploadFileValidator,
            TransactionalFileLifecycle transactionalFileLifecycle,
            @org.springframework.beans.factory.annotation.Value("${app.upload.max-photo-size-mb}") long maxPhotoSizeMb,
            @org.springframework.beans.factory.annotation.Value("${app.upload.max-files-per-request:10}") int maxFilesPerRequest
    ) {
        this.fotoObjetoMuseoRepository = fotoObjetoMuseoRepository;
        this.objetoMuseoService = objetoMuseoService;
        this.objectFileStorageService = objectFileStorageService;
        this.imageWatermarkService = imageWatermarkService;
        this.uploadFileValidator = uploadFileValidator;
        this.transactionalFileLifecycle = transactionalFileLifecycle;
        this.maxSizeBytes = maxPhotoSizeMb * 1024L * 1024L;
        this.maxFilesPerRequest = maxFilesPerRequest;
    }

    @Transactional
    public FotoObjetoMuseoResponseDTO subir(Long objetoId, MultipartFile archivo, String descripcion, String cargadoPor) {
        return subir(objetoId, archivo, descripcion, VisibilidadCampo.PUBLICO, cargadoPor);
    }

    @Transactional
    public FotoObjetoMuseoResponseDTO subir(Long objetoId, MultipartFile archivo, String descripcion, VisibilidadCampo visibilidad, String cargadoPor) {
        ObjetoMuseo objeto = objetoMuseoService.buscarObjetoActivo(objetoId);
        UploadFileValidator.ValidatedFile validated = validarArchivo(archivo);
        return guardar(objeto, validated, descripcion, visibilidad, cargadoPor);
    }

    @Transactional
    public List<FotoObjetoMuseoResponseDTO> subirTodos(
            Long objetoId,
            List<MultipartFile> archivos,
            String descripcion,
            List<VisibilidadCampo> visibilidades,
            VisibilidadCampo visibilidad,
            String cargadoPor
    ) {
        validarCantidad(archivos);
        ObjetoMuseo objeto = objetoMuseoService.buscarObjetoActivo(objetoId);
        List<UploadFileValidator.ValidatedFile> validatedFiles = archivos.stream().map(this::validarArchivo).toList();
        List<FotoObjetoMuseoResponseDTO> result = new ArrayList<>();
        for (int index = 0; index < validatedFiles.size(); index++) {
            VisibilidadCampo itemVisibility = visibilidades != null && index < visibilidades.size()
                    ? visibilidades.get(index)
                    : visibilidad;
            result.add(guardar(objeto, validatedFiles.get(index), descripcion, itemVisibility, cargadoPor));
        }
        return result;
    }

    private FotoObjetoMuseoResponseDTO guardar(
            ObjetoMuseo objeto,
            UploadFileValidator.ValidatedFile validated,
            String descripcion,
            VisibilidadCampo visibilidad,
            String cargadoPor
    ) {
        byte[] original = validated.bytes();
        ImageWatermarkService.GeneratedPublicImage publicImage = imageWatermarkService.generar(original, validated.contentType());
        ObjectFileStorageService.StoredObjectFile storedOriginal = null;
        ObjectFileStorageService.StoredObjectFile storedPublic = null;
        try {
            storedOriginal = objectFileStorageService.storeBytes(
                    objeto.getId(), "fotos/original", original,
                    objeto.getNumeroInventario() + "." + validated.extension(), validated.extension());
            storedPublic = objectFileStorageService.storeBytes(
                    objeto.getId(), "fotos/public", publicImage.bytes(),
                    objeto.getNumeroInventario() + "." + publicImage.extension(), publicImage.extension());
            registrarLimpiezaSiRollback(storedOriginal.relativePath(), storedPublic.relativePath());

            FotoObjetoMuseo foto = new FotoObjetoMuseo();
            foto.setObjetoMuseo(objeto);
            foto.setNombreArchivo(storedOriginal.originalName());
            foto.setNombreArchivoOriginal(validated.originalName());
            foto.setNombreArchivoAlmacenado(storedOriginal.storedName());
            foto.setContentType(validated.contentType());
            foto.setTamanioBytes((long) original.length);
            foto.setRutaAlmacenamiento(storedOriginal.absolutePath());
            foto.setRutaRelativa(storedOriginal.relativePath());
            foto.setRutaPublica(storedPublic.relativePath());
            foto.setContentTypePublico(publicImage.contentType());
            foto.setTamanioBytesPublico((long) publicImage.bytes().length);
            foto.setDescripcion(descripcion);
            foto.setVisibilidad(visibilidad == null ? VisibilidadCampo.PUBLICO : visibilidad);
            foto.setFechaCarga(com.proveedores.time.MuseoTime.now());
            foto.setCargadoPor(cargadoPor);
            return toResponse(fotoObjetoMuseoRepository.save(foto));
        } catch (RuntimeException ex) {
            if (storedPublic != null) objectFileStorageService.delete(storedPublic.relativePath());
            if (storedOriginal != null) objectFileStorageService.delete(storedOriginal.relativePath());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<FotoObjetoMuseoResponseDTO> listar(Long objetoId) {
        objetoMuseoService.buscarObjetoActivo(objetoId);
        return fotoObjetoMuseoRepository.findByObjetoMuseoIdAndEliminadoFalse(objetoId).stream()
                .filter(this::puedeVerFoto)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FotoArchivo descargar(Long objetoId, Long fotoId) {
        FotoObjetoMuseo foto = buscarFoto(objetoId, fotoId);
        validarPuedeVerFoto(foto);
        if (!StringUtils.hasText(foto.getRutaPublica())) generarVersionPublica(foto);
        Resource resource = objectFileStorageService.load(foto.getRutaPublica());
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Archivo de foto no encontrado");
        }
        return new FotoArchivo(toResponse(foto), resource, foto.getContentTypePublico(), false,
                imageWatermarkService.publicCacheMaxAgeSeconds());
    }

    @Transactional(readOnly = true)
    public FotoArchivo descargarOriginal(Long objetoId, Long fotoId) {
        if (!puedeDescargarOriginal()) throw new AccessDeniedException("No tiene permiso para descargar la fotografia original");
        FotoObjetoMuseo foto = buscarFoto(objetoId, fotoId);
        validarPuedeVerFoto(foto);
        return new FotoArchivo(toResponse(foto), cargarOriginal(foto), foto.getContentType(), true, 0);
    }

    @Transactional
    public FotoObjetoMuseoResponseDTO actualizarVisibilidad(Long objetoId, Long fotoId, VisibilidadCampo visibilidad) {
        FotoObjetoMuseo foto = buscarFoto(objetoId, fotoId);
        foto.setVisibilidad(visibilidad == null ? VisibilidadCampo.PUBLICO : visibilidad);
        return toResponse(fotoObjetoMuseoRepository.save(foto));
    }

    @Transactional
    public void eliminar(Long objetoId, Long fotoId) {
        FotoObjetoMuseo foto = buscarFoto(objetoId, fotoId);
        foto.setActivo(false);
        foto.setEliminado(true);
        foto.setFechaEliminacion(com.proveedores.time.MuseoTime.now());
        fotoObjetoMuseoRepository.save(foto);
    }

    public RegeneracionFotosPublicasResponseDTO regenerarPublicasFaltantes() {
        int generadas = 0;
        int omitidas = 0;
        int errores = 0;
        List<FotoObjetoMuseo> fotos = new ArrayList<>(fotoObjetoMuseoRepository.findByEliminadoFalseOrderByIdAsc());
        for (FotoObjetoMuseo foto : fotos) {
            try {
                if (StringUtils.hasText(foto.getRutaPublica()) && objectFileStorageService.exists(foto.getRutaPublica())) {
                    omitidas++;
                } else {
                    generarVersionPublica(foto);
                    generadas++;
                }
            } catch (RuntimeException ex) {
                errores++;
            }
        }
        return new RegeneracionFotosPublicasResponseDTO(fotos.size(), generadas, omitidas, errores);
    }

    private FotoObjetoMuseo buscarFoto(Long objetoId, Long fotoId) {
        objetoMuseoService.buscarObjetoActivo(objetoId);
        return fotoObjetoMuseoRepository.findByIdAndObjetoMuseoIdAndEliminadoFalse(fotoId, objetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Foto del objeto no encontrada"));
    }

    private void validarPuedeVerFoto(FotoObjetoMuseo foto) {
        if (!puedeVerFoto(foto)) {
            throw new ResourceNotFoundException("Foto del objeto no encontrada");
        }
    }

    private boolean puedeVerFoto(FotoObjetoMuseo foto) {
        return foto.getVisibilidad() != VisibilidadCampo.PRIVADO || puedeVerPrivados();
    }

    private boolean puedeVerPrivados() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority)
                        || "ROLE_MUSEOLOGO".equals(authority));
    }

    private UploadFileValidator.ValidatedFile validarArchivo(MultipartFile archivo) {
        return uploadFileValidator.validateImage(archivo, maxSizeBytes, new UploadFileValidator.UploadMessages(
                "La foto es obligatoria",
                "Tipo de imagen no permitido",
                "La foto supera el tamano maximo permitido",
                "El archivo no contiene una imagen valida",
                "El contenido de la imagen no coincide con su tipo MIME",
                "No se pudo leer la foto"
        ));
    }

    private void validarCantidad(List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty()) throw new BusinessException("Debe seleccionar al menos una foto");
        if (archivos.size() > maxFilesPerRequest) {
            throw new BusinessException("La carga supera la cantidad maxima de archivos permitida");
        }
    }

    private boolean puedeDescargarOriginal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private Resource cargarOriginal(FotoObjetoMuseo foto) {
        return StringUtils.hasText(foto.getRutaRelativa())
                ? objectFileStorageService.load(foto.getRutaRelativa())
                : new org.springframework.core.io.FileSystemResource(foto.getRutaAlmacenamiento());
    }

    private void generarVersionPublica(FotoObjetoMuseo foto) {
        ObjectFileStorageService.StoredObjectFile stored = null;
        try {
            byte[] original = cargarOriginal(foto).getInputStream().readAllBytes();
            ImageWatermarkService.GeneratedPublicImage generated = imageWatermarkService.generar(original, foto.getContentType());
            stored = objectFileStorageService.storeBytes(
                    foto.getObjetoMuseo().getId(), "fotos/public", generated.bytes(), foto.getNombreArchivo(), generated.extension());
            registrarLimpiezaSiRollback(stored.relativePath());
            foto.setRutaPublica(stored.relativePath());
            foto.setContentTypePublico(generated.contentType());
            foto.setTamanioBytesPublico((long) generated.bytes().length);
            fotoObjetoMuseoRepository.save(foto);
        } catch (IOException ex) {
            if (stored != null) objectFileStorageService.delete(stored.relativePath());
            throw new BusinessException("No se pudo leer la fotografia original");
        } catch (RuntimeException ex) {
            if (stored != null) objectFileStorageService.delete(stored.relativePath());
            throw ex;
        }
    }

    private void registrarLimpiezaSiRollback(String... relativePaths) {
        for (String path : relativePaths) {
            transactionalFileLifecycle.deleteOnRollback(() -> objectFileStorageService.delete(path));
        }
    }

    private FotoObjetoMuseoResponseDTO toResponse(FotoObjetoMuseo foto) {
        return new FotoObjetoMuseoResponseDTO(
                foto.getId(),
                foto.getObjetoMuseo().getId(),
                foto.getNombreArchivo(),
                null,
                foto.getContentType(),
                foto.getTamanioBytes(),
                foto.getDescripcion(),
                foto.getVisibilidad(),
                foto.getFechaCarga(),
                foto.getCargadoPor()
        );
    }

    public record FotoArchivo(FotoObjetoMuseoResponseDTO metadata, Resource resource, String contentType, boolean original, long cacheMaxAgeSeconds) {
    }
}
