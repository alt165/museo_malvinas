package com.proveedores.service;

import com.proveedores.dto.ReciboEscaneadoObjetoMuseoResponseDTO;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.ReciboEscaneadoObjetoMuseo;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.ReciboEscaneadoObjetoMuseoRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReciboEscaneadoObjetoMuseoService {

    private static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of("application/pdf", "image/jpeg", "image/png", "image/webp");

    private final ReciboEscaneadoObjetoMuseoRepository reciboEscaneadoRepository;
    private final ObjetoMuseoService objetoMuseoService;
    private final ObjectFileStorageService objectFileStorageService;
    private final long maxSizeBytes;

    public ReciboEscaneadoObjetoMuseoService(
            ReciboEscaneadoObjetoMuseoRepository reciboEscaneadoRepository,
            ObjetoMuseoService objetoMuseoService,
            ObjectFileStorageService objectFileStorageService,
            @org.springframework.beans.factory.annotation.Value("${app.upload.max-receipt-size-mb}") long maxReceiptSizeMb
    ) {
        this.reciboEscaneadoRepository = reciboEscaneadoRepository;
        this.objetoMuseoService = objetoMuseoService;
        this.objectFileStorageService = objectFileStorageService;
        this.maxSizeBytes = maxReceiptSizeMb * 1024L * 1024L;
    }

    @Transactional
    public ReciboEscaneadoObjetoMuseoResponseDTO subir(Long objetoId, MultipartFile archivo, String cargadoPor) {
        ObjetoMuseo objeto = objetoMuseoService.buscarObjetoActivo(objetoId);
        validarArchivo(archivo);

        reciboEscaneadoRepository.findFirstByObjetoMuseoIdAndEliminadoFalseOrderByFechaCargaDesc(objetoId)
                .ifPresent(this::eliminarActivo);
        reciboEscaneadoRepository.flush();

        ObjectFileStorageService.StoredObjectFile storedFile = objectFileStorageService.store(objetoId, "recibos", archivo);
        ReciboEscaneadoObjetoMuseo recibo = new ReciboEscaneadoObjetoMuseo();
        recibo.setObjetoMuseo(objeto);
        recibo.setNombreArchivoOriginal(storedFile.originalName());
        recibo.setNombreArchivoAlmacenado(storedFile.storedName());
        recibo.setContentType(archivo.getContentType());
        recibo.setTamanioBytes(archivo.getSize());
        recibo.setRutaRelativa(storedFile.relativePath());
        recibo.setFechaCarga(LocalDateTime.now());
        recibo.setCargadoPor(cargadoPor);
        return toResponse(reciboEscaneadoRepository.save(recibo));
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

    @Transactional
    public void eliminar(Long objetoId, Long reciboId) {
        ReciboEscaneadoObjetoMuseo recibo = reciboEscaneadoRepository.findByIdAndObjetoMuseoIdAndEliminadoFalse(reciboId, objetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Recibo escaneado no encontrado"));
        eliminarActivo(recibo);
    }

    private void eliminarActivo(ReciboEscaneadoObjetoMuseo recibo) {
        recibo.setActivo(false);
        recibo.setEliminado(true);
        recibo.setFechaEliminacion(LocalDateTime.now());
        reciboEscaneadoRepository.save(recibo);
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("El recibo escaneado esta vacio");
        }
        if (!CONTENT_TYPES_PERMITIDOS.contains(archivo.getContentType())) {
            throw new BusinessException("Tipo de archivo no permitido para recibo escaneado");
        }
        if (archivo.getSize() > maxSizeBytes) {
            throw new BusinessException("El recibo escaneado supera el tamano maximo permitido");
        }
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
