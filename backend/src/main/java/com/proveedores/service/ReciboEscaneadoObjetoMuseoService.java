package com.proveedores.service;

import com.proveedores.dto.ReciboEscaneadoObjetoMuseoResponseDTO;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.ReciboEscaneadoObjetoMuseo;
import com.proveedores.entity.TipoOperacionAuditoria;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.ReciboEscaneadoObjetoMuseoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReciboEscaneadoObjetoMuseoService {

    private final ReciboEscaneadoObjetoMuseoRepository reciboEscaneadoRepository;
    private final ObjetoMuseoService objetoMuseoService;
    private final ObjectFileStorageService objectFileStorageService;
    private final UploadFileValidator uploadFileValidator;
    private final TransactionalFileLifecycle transactionalFileLifecycle;
    private final AuditoriaObjetoService auditoriaService;
    private final long maxSizeBytes;

    public ReciboEscaneadoObjetoMuseoService(
            ReciboEscaneadoObjetoMuseoRepository reciboEscaneadoRepository,
            ObjetoMuseoService objetoMuseoService,
            ObjectFileStorageService objectFileStorageService,
            UploadFileValidator uploadFileValidator,
            TransactionalFileLifecycle transactionalFileLifecycle,
            AuditoriaObjetoService auditoriaService,
            @org.springframework.beans.factory.annotation.Value("${app.upload.max-receipt-size-mb}") long maxReceiptSizeMb
    ) {
        this.reciboEscaneadoRepository = reciboEscaneadoRepository;
        this.objetoMuseoService = objetoMuseoService;
        this.objectFileStorageService = objectFileStorageService;
        this.uploadFileValidator = uploadFileValidator;
        this.transactionalFileLifecycle = transactionalFileLifecycle;
        this.auditoriaService = auditoriaService;
        this.maxSizeBytes = maxReceiptSizeMb * 1024L * 1024L;
    }

    @Transactional
    public ReciboEscaneadoObjetoMuseoResponseDTO subir(Long objetoId, MultipartFile archivo, String cargadoPor) {
        ObjetoMuseo objeto = objetoMuseoService.buscarObjetoActivo(objetoId);
        UploadFileValidator.ValidatedFile validated = validarArchivo(archivo);

        ReciboEscaneadoObjetoMuseo anterior = reciboEscaneadoRepository
                .findFirstByObjetoMuseoIdAndEliminadoFalseOrderByFechaCargaDesc(objetoId)
                .orElse(null);

        ObjectFileStorageService.StoredObjectFile storedFile = objectFileStorageService.storeBytes(
                objetoId, "recibos", validated.bytes(), validated.originalName(), validated.extension());
        transactionalFileLifecycle.deleteOnRollback(() -> objectFileStorageService.delete(storedFile.relativePath()));

        if (anterior != null) {
            eliminarActivo(anterior);
            reciboEscaneadoRepository.flush();
        }

        ReciboEscaneadoObjetoMuseo recibo = new ReciboEscaneadoObjetoMuseo();
        recibo.setObjetoMuseo(objeto);
        recibo.setNombreArchivoOriginal(validated.originalName());
        recibo.setNombreArchivoAlmacenado(storedFile.storedName());
        recibo.setContentType(validated.contentType());
        recibo.setTamanioBytes((long) validated.bytes().length);
        recibo.setRutaRelativa(storedFile.relativePath());
        recibo.setFechaCarga(com.proveedores.time.MuseoTime.now());
        recibo.setCargadoPor(cargadoPor);
        ReciboEscaneadoObjetoMuseo saved = reciboEscaneadoRepository.saveAndFlush(recibo);
        if (anterior != null) {
            transactionalFileLifecycle.deleteAfterCommit(() -> objectFileStorageService.delete(anterior.getRutaRelativa()));
        }
        auditoriaService.registrarEvento("RECIBO_ESCANEADO", saved.getId(), null, TipoOperacionAuditoria.CREACION,
                anterior == null ? "RECIBO_CARGADO" : "RECIBO_REEMPLAZADO",
                "Carga de recibo escaneado", "ARCHIVOS", null,
                auditoriaService.mapOf("objetoId", objetoId, "contentType", saved.getContentType(),
                        "tamanioBytes", saved.getTamanioBytes()), cargadoPor);
        return toResponse(saved);
    }

    @Transactional
    public ReciboEscaneadoObjetoMuseoResponseDTO agregar(Long objetoId, MultipartFile archivo, String cargadoPor) {
        ObjetoMuseo objeto = objetoMuseoService.buscarObjetoActivo(objetoId);
        UploadFileValidator.ValidatedFile validated = validarArchivo(archivo);
        ObjectFileStorageService.StoredObjectFile storedFile = objectFileStorageService.storeBytes(
                objetoId, "recibos", validated.bytes(), validated.originalName(), validated.extension());
        transactionalFileLifecycle.deleteOnRollback(() -> objectFileStorageService.delete(storedFile.relativePath()));

        ReciboEscaneadoObjetoMuseo recibo = new ReciboEscaneadoObjetoMuseo();
        recibo.setObjetoMuseo(objeto);
        recibo.setNombreArchivoOriginal(validated.originalName());
        recibo.setNombreArchivoAlmacenado(storedFile.storedName());
        recibo.setContentType(validated.contentType());
        recibo.setTamanioBytes((long) validated.bytes().length);
        recibo.setRutaRelativa(storedFile.relativePath());
        recibo.setFechaCarga(com.proveedores.time.MuseoTime.now());
        recibo.setCargadoPor(cargadoPor);
        ReciboEscaneadoObjetoMuseo saved = reciboEscaneadoRepository.saveAndFlush(recibo);
        auditoriaService.registrarEvento("RECIBO_ESCANEADO", saved.getId(), null, TipoOperacionAuditoria.CREACION,
                "RECIBO_CARGADO", "Carga de recibo escaneado", "ARCHIVOS", null,
                auditoriaService.mapOf("objetoId", objetoId, "contentType", saved.getContentType(),
                        "tamanioBytes", saved.getTamanioBytes()), cargadoPor);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReciboEscaneadoObjetoMuseoResponseDTO> listar(Long objetoId) {
        objetoMuseoService.buscarObjetoActivo(objetoId);
        return reciboEscaneadoRepository.findByObjetoMuseoIdAndEliminadoFalseOrderByFechaCargaDescIdDesc(objetoId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ReciboEscaneadoObjetoMuseoResponseDTO> obtener(Long objetoId) {
        objetoMuseoService.buscarObjetoActivo(objetoId);
        return reciboEscaneadoRepository.findFirstByObjetoMuseoIdAndEliminadoFalseOrderByFechaCargaDesc(objetoId)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReciboEscaneadoArchivo descargar(Long objetoId) {
        ReciboEscaneadoObjetoMuseo recibo = reciboEscaneadoRepository.findFirstByObjetoMuseoIdAndEliminadoFalseOrderByFechaCargaDesc(objetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Recibo escaneado no encontrado"));
        Resource resource = objectFileStorageService.load(recibo.getRutaRelativa());
        return new ReciboEscaneadoArchivo(toResponse(recibo), resource, recibo.getContentType(), recibo.getNombreArchivoOriginal());
    }

    @Transactional(readOnly = true)
    public ReciboEscaneadoArchivo descargar(Long objetoId, Long reciboId) {
        objetoMuseoService.buscarObjetoActivo(objetoId);
        ReciboEscaneadoObjetoMuseo recibo = reciboEscaneadoRepository.findByIdAndObjetoMuseoIdAndEliminadoFalse(reciboId, objetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Recibo escaneado no encontrado"));
        Resource resource = objectFileStorageService.load(recibo.getRutaRelativa());
        return new ReciboEscaneadoArchivo(toResponse(recibo), resource, recibo.getContentType(), recibo.getNombreArchivoOriginal());
    }

    @Transactional
    public void eliminar(Long objetoId, Long reciboId) {
        ReciboEscaneadoObjetoMuseo recibo = reciboEscaneadoRepository.findByIdAndObjetoMuseoIdAndEliminadoFalse(reciboId, objetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Recibo escaneado no encontrado"));
        eliminarActivo(recibo);
        auditoriaService.registrarEvento("RECIBO_ESCANEADO", recibo.getId(), null, TipoOperacionAuditoria.ELIMINACION,
                "RECIBO_ELIMINADO", "Baja lógica de recibo escaneado", "ARCHIVOS",
                auditoriaService.mapOf("objetoId", objetoId), null, null);
    }

    private void eliminarActivo(ReciboEscaneadoObjetoMuseo recibo) {
        recibo.setActivo(false);
        recibo.setEliminado(true);
        recibo.setFechaEliminacion(com.proveedores.time.MuseoTime.now());
        reciboEscaneadoRepository.save(recibo);
    }

    private UploadFileValidator.ValidatedFile validarArchivo(MultipartFile archivo) {
        return uploadFileValidator.validateImageOrPdf(archivo, maxSizeBytes, new UploadFileValidator.UploadMessages(
                "El recibo escaneado esta vacio",
                "Tipo de archivo no permitido para recibo escaneado",
                "El recibo escaneado supera el tamano maximo permitido",
                "El recibo escaneado no contiene un PDF o imagen valida",
                "El contenido del recibo no coincide con su tipo MIME",
                "No se pudo leer el recibo escaneado"
        ));
    }

    public ReciboEscaneadoObjetoMuseoResponseDTO toResponse(ReciboEscaneadoObjetoMuseo recibo) {
        return new ReciboEscaneadoObjetoMuseoResponseDTO(
                recibo.getId(),
                recibo.getObjetoMuseo().getId(),
                recibo.getNombreArchivoOriginal(),
                recibo.getContentType(),
                recibo.getTamanioBytes(),
                recibo.getFechaCarga(),
                recibo.getCargadoPor()
        );
    }

    public record ReciboEscaneadoArchivo(
            ReciboEscaneadoObjetoMuseoResponseDTO metadata,
            Resource resource,
            String contentType,
            String nombreArchivo
    ) {
    }
}
