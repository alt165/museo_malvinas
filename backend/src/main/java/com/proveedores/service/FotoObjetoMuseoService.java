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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FotoObjetoMuseoService {

    private static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");

    private final FotoObjetoMuseoRepository fotoObjetoMuseoRepository;
    private final ObjetoMuseoService objetoMuseoService;
    private final ObjectFileStorageService objectFileStorageService;
    private final ImageWatermarkService imageWatermarkService;
    private final long maxSizeBytes;

    public FotoObjetoMuseoService(
            FotoObjetoMuseoRepository fotoObjetoMuseoRepository,
            ObjetoMuseoService objetoMuseoService,
            ObjectFileStorageService objectFileStorageService,
            ImageWatermarkService imageWatermarkService,
            @org.springframework.beans.factory.annotation.Value("${app.upload.max-photo-size-mb}") long maxPhotoSizeMb
    ) {
        this.fotoObjetoMuseoRepository = fotoObjetoMuseoRepository;
        this.objetoMuseoService = objetoMuseoService;
        this.objectFileStorageService = objectFileStorageService;
        this.imageWatermarkService = imageWatermarkService;
        this.maxSizeBytes = maxPhotoSizeMb * 1024L * 1024L;
    }

    @Transactional
    public FotoObjetoMuseoResponseDTO subir(Long objetoId, MultipartFile archivo, String descripcion, String cargadoPor) {
        return subir(objetoId, archivo, descripcion, VisibilidadCampo.PUBLICO, cargadoPor);
    }

    @Transactional
    public FotoObjetoMuseoResponseDTO subir(Long objetoId, MultipartFile archivo, String descripcion, VisibilidadCampo visibilidad, String cargadoPor) {
        ObjetoMuseo objeto = objetoMuseoService.buscarObjetoActivo(objetoId);
        validarArchivo(archivo);

        byte[] original = leerArchivo(archivo);
        ImageWatermarkService.GeneratedPublicImage publicImage = imageWatermarkService.generar(original, archivo.getContentType());
        String extension = extensionOriginal(archivo.getContentType());
        ObjectFileStorageService.StoredObjectFile storedOriginal = null;
        ObjectFileStorageService.StoredObjectFile storedPublic = null;
        try {
            storedOriginal = objectFileStorageService.storeBytes(
                    objetoId, "fotos/original", original, objeto.getNumeroInventario() + "." + extension, extension);
            storedPublic = objectFileStorageService.storeBytes(
                    objetoId, "fotos/public", publicImage.bytes(),
                    objeto.getNumeroInventario() + "." + publicImage.extension(), publicImage.extension());
            registrarLimpiezaSiRollback(storedOriginal.relativePath(), storedPublic.relativePath());

            FotoObjetoMuseo foto = new FotoObjetoMuseo();
            foto.setObjetoMuseo(objeto);
            foto.setNombreArchivo(storedOriginal.originalName());
            foto.setNombreArchivoOriginal(archivo.getOriginalFilename());
            foto.setNombreArchivoAlmacenado(storedOriginal.storedName());
            foto.setContentType(archivo.getContentType());
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

    private byte[] leerArchivo(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException ex) {
            throw new BusinessException("No se pudo leer la foto");
        }
    }

    private String extensionOriginal(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    private void registrarLimpiezaSiRollback(String... relativePaths) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    for (String path : relativePaths) objectFileStorageService.delete(path);
                }
            }
        });
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
