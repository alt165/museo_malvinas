package com.proveedores.service;

import com.proveedores.dto.ReciboIngresoObjetoResponseDTO;
import com.proveedores.entity.ReciboIngresoObjeto;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.ReciboIngresoObjetoRepository;
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
public class ReciboIngresoObjetoService {

    private static final Set<String> CONTENT_TYPES_COPIA_FIRMADA = Set.of("application/pdf", "image/jpeg", "image/png", "image/webp");

    private final ReciboIngresoObjetoRepository reciboIngresoObjetoRepository;
    private final ReciboPdfService reciboPdfService;
    private final Path signedReceiptsDir;
    private final long maxSignedReceiptSizeBytes;

    public ReciboIngresoObjetoService(
            ReciboIngresoObjetoRepository reciboIngresoObjetoRepository,
            ReciboPdfService reciboPdfService,
            @Value("${app.storage.signed-receipts-dir}") String signedReceiptsDir,
            @Value("${app.upload.max-signed-receipt-size-mb}") long maxSignedReceiptSizeMb
    ) {
        this.reciboIngresoObjetoRepository = reciboIngresoObjetoRepository;
        this.reciboPdfService = reciboPdfService;
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
        validarCopiaFirmada(archivo);

        String nombreOriginal = StringUtils.hasText(archivo.getOriginalFilename()) ? archivo.getOriginalFilename() : "recibo-firmado";
        String nombreSeguro = nombreOriginal.replaceAll("[^A-Za-z0-9._-]", "_");
        Path directorioRecibo = signedReceiptsDir.resolve(String.valueOf(id)).normalize();
        Path destino = directorioRecibo.resolve(UUID.randomUUID() + "-" + nombreSeguro).normalize();
        if (!destino.startsWith(signedReceiptsDir)) {
            throw new BusinessException("Nombre de archivo invalido");
        }

        try {
            Files.createDirectories(directorioRecibo);
            archivo.transferTo(destino);
        } catch (IOException ex) {
            throw new BusinessException("No se pudo almacenar la copia firmada del recibo");
        }

        recibo.setCopiaFirmadaNombreArchivo(nombreOriginal);
        recibo.setCopiaFirmadaContentType(archivo.getContentType());
        recibo.setCopiaFirmadaTamanioBytes(archivo.getSize());
        recibo.setCopiaFirmadaRutaAlmacenamiento(destino.toString());
        recibo.setCopiaFirmadaFechaCarga(LocalDateTime.now());
        recibo.setCopiaFirmadaCargadoPor(cargadoPor);
        return toResponse(reciboIngresoObjetoRepository.save(recibo));
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

    private void validarCopiaFirmada(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("La copia firmada es obligatoria");
        }
        if (!CONTENT_TYPES_COPIA_FIRMADA.contains(archivo.getContentType())) {
            throw new BusinessException("Tipo de archivo no permitido para copia firmada");
        }
        if (archivo.getSize() > maxSignedReceiptSizeBytes) {
            throw new BusinessException("La copia firmada supera el tamano maximo permitido");
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
