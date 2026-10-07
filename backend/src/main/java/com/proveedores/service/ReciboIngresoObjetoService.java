package com.proveedores.service;

import com.proveedores.dto.ReciboIngresoObjetoResponseDTO;
import com.proveedores.entity.ReciboIngresoObjeto;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.ReciboIngresoObjetoRepository;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReciboIngresoObjetoService {

    private final ReciboIngresoObjetoRepository reciboIngresoObjetoRepository;
    private final ReciboPdfService reciboPdfService;
    private final UploadFileValidator uploadFileValidator;
    private final TransactionalFileLifecycle transactionalFileLifecycle;
    private final Path signedReceiptsDir;
    private final long maxSignedReceiptSizeBytes;

    public ReciboIngresoObjetoService(
            ReciboIngresoObjetoRepository reciboIngresoObjetoRepository,
            ReciboPdfService reciboPdfService,
            UploadFileValidator uploadFileValidator,
            TransactionalFileLifecycle transactionalFileLifecycle,
            @Value("${app.storage.signed-receipts-dir}") String signedReceiptsDir,
            @Value("${app.upload.max-signed-receipt-size-mb}") long maxSignedReceiptSizeMb
    ) {
        this.reciboIngresoObjetoRepository = reciboIngresoObjetoRepository;
        this.reciboPdfService = reciboPdfService;
        this.uploadFileValidator = uploadFileValidator;
        this.transactionalFileLifecycle = transactionalFileLifecycle;
        this.signedReceiptsDir = Path.of(signedReceiptsDir).toAbsolutePath().normalize();
        this.maxSignedReceiptSizeBytes = maxSignedReceiptSizeMb * 1024L * 1024L;
    }

    @Transactional(readOnly = true)
    public ReciboIngresoObjetoResponseDTO obtener(Long id) {
        return toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<ReciboIngresoObjetoResponseDTO> listarPorObjeto(Long objetoId) {
        return reciboIngresoObjetoRepository.findByObjetoMuseoIdAndEliminadoFalse(objetoId).stream()
                .map(ReciboIngresoObjetoService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] generarPdf(Long id) {
        return reciboPdfService.generar(buscarActivo(id));
    }

    @Transactional
    public ReciboIngresoObjetoResponseDTO subirCopiaFirmada(Long id, MultipartFile archivo, String cargadoPor) {
        ReciboIngresoObjeto recibo = buscarActivo(id);
        UploadFileValidator.ValidatedFile validated = validarCopiaFirmada(archivo);
        Path rutaAnterior = StringUtils.hasText(recibo.getCopiaFirmadaRutaAlmacenamiento())
                ? Path.of(recibo.getCopiaFirmadaRutaAlmacenamiento()).toAbsolutePath().normalize()
                : null;
        Path destino = almacenarCopiaFirmada(id, validated);
        transactionalFileLifecycle.deleteOnRollback(() -> eliminarArchivo(destino));

        recibo.setCopiaFirmadaNombreArchivo(validated.originalName());
        recibo.setCopiaFirmadaContentType(validated.contentType());
        recibo.setCopiaFirmadaTamanioBytes((long) validated.bytes().length);
        recibo.setCopiaFirmadaRutaAlmacenamiento(destino.toString());
        recibo.setCopiaFirmadaFechaCarga(com.proveedores.time.MuseoTime.now());
        recibo.setCopiaFirmadaCargadoPor(cargadoPor);
        ReciboIngresoObjeto saved = reciboIngresoObjetoRepository.saveAndFlush(recibo);
        if (rutaAnterior != null && !rutaAnterior.equals(destino)) {
            transactionalFileLifecycle.deleteAfterCommit(() -> eliminarArchivo(rutaAnterior));
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReciboArchivo descargarCopiaFirmada(Long id) {
        ReciboIngresoObjeto recibo = buscarActivo(id);
        if (!StringUtils.hasText(recibo.getCopiaFirmadaRutaAlmacenamiento())) {
            throw new ResourceNotFoundException("Copia firmada del recibo no encontrada");
        }
        Resource resource = new FileSystemResource(recibo.getCopiaFirmadaRutaAlmacenamiento());
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Archivo de copia firmada no encontrado");
        }
        return new ReciboArchivo(toResponse(recibo), resource, recibo.getCopiaFirmadaContentType(), recibo.getCopiaFirmadaNombreArchivo());
    }

    private ReciboIngresoObjeto buscarActivo(Long id) {
        ReciboIngresoObjeto recibo = reciboIngresoObjetoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recibo no encontrado"));
        if (recibo.getEliminado()) {
            throw new ResourceNotFoundException("Recibo no encontrado");
        }
        return recibo;
    }

    private UploadFileValidator.ValidatedFile validarCopiaFirmada(MultipartFile archivo) {
        return uploadFileValidator.validateImageOrPdf(archivo, maxSignedReceiptSizeBytes, new UploadFileValidator.UploadMessages(
                "La copia firmada es obligatoria",
                "Tipo de archivo no permitido para copia firmada",
                "La copia firmada supera el tamano maximo permitido",
                "La copia firmada no contiene un PDF o imagen valida",
                "El contenido de la copia firmada no coincide con su tipo MIME",
                "No se pudo leer la copia firmada"
        ));
    }

    private Path almacenarCopiaFirmada(Long id, UploadFileValidator.ValidatedFile file) {
        Path directorioRecibo = signedReceiptsDir.resolve(String.valueOf(id)).normalize();
        Path destino = directorioRecibo.resolve(UUID.randomUUID() + "." + file.extension()).normalize();
        if (!directorioRecibo.startsWith(signedReceiptsDir) || !destino.startsWith(signedReceiptsDir)) {
            throw new BusinessException("Nombre de archivo invalido");
        }
        Path temporary = null;
        try {
            Files.createDirectories(directorioRecibo);
            temporary = Files.createTempFile(directorioRecibo, ".upload-", ".tmp");
            Files.write(temporary, file.bytes());
            try {
                Files.move(temporary, destino, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destino);
            }
            return destino;
        } catch (IOException exception) {
            eliminarArchivo(temporary);
            throw new BusinessException("No se pudo almacenar la copia firmada del recibo");
        }
    }

    private void eliminarArchivo(Path path) {
        if (path == null) return;
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(signedReceiptsDir)) {
            throw new BusinessException("Ruta de archivo invalida");
        }
        try {
            Files.deleteIfExists(normalized);
        } catch (IOException exception) {
            throw new BusinessException("No se pudo eliminar la copia firmada almacenada");
        }
    }

    public static ReciboIngresoObjetoResponseDTO toResponse(ReciboIngresoObjeto recibo) {
        return new ReciboIngresoObjetoResponseDTO(
                recibo.getId(),
                recibo.getNumeroRecibo(),
                recibo.getFechaEmision(),
                recibo.getObjetoMuseo().getId(),
                recibo.getDepositante().getId(),
                recibo.getNumeroInventario(),
                recibo.getDenominacionObjeto(),
                recibo.getDescripcionBreve(),
                recibo.getDepositanteNombre(),
                recibo.getDepositanteContacto(),
                recibo.getOperador(),
                recibo.getTextoConstancia(),
                StringUtils.hasText(recibo.getCopiaFirmadaRutaAlmacenamiento()),
                recibo.getCopiaFirmadaNombreArchivo(),
                recibo.getCopiaFirmadaFechaCarga(),
                recibo.getCopiaFirmadaCargadoPor()
        );
    }

    public record ReciboArchivo(ReciboIngresoObjetoResponseDTO metadata, Resource resource, String contentType, String nombreArchivo) {
    }
}
