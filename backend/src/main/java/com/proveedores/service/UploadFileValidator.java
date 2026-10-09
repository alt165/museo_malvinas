package com.proveedores.service;

import com.proveedores.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class UploadFileValidator {

    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final long maxImagePixels;

    public UploadFileValidator(@Value("${app.upload.max-image-pixels:100000000}") long maxImagePixels) {
        this.maxImagePixels = maxImagePixels;
    }

    public ValidatedFile validateImage(MultipartFile file, long maxSizeBytes, UploadMessages messages) {
        validatePresenceSizeAndDeclaredType(file, maxSizeBytes, IMAGE_CONTENT_TYPES, messages);
        byte[] bytes = read(file, messages.readError());
        DetectedImage image = detectImage(bytes, messages.invalidContent());
        if (!image.contentType().equalsIgnoreCase(file.getContentType())) {
            throw new BusinessException(messages.mimeMismatch());
        }
        return new ValidatedFile(bytes, image.contentType(), image.extension(), originalName(file));
    }

    public ValidatedFile validateImageOrPdf(MultipartFile file, long maxSizeBytes, UploadMessages messages) {
        Set<String> allowed = Set.of("image/jpeg", "image/png", "image/webp", PDF_CONTENT_TYPE);
        validatePresenceSizeAndDeclaredType(file, maxSizeBytes, allowed, messages);
        byte[] bytes = read(file, messages.readError());
        if (PDF_CONTENT_TYPE.equalsIgnoreCase(file.getContentType())) {
            validatePdf(bytes, messages.invalidContent());
            return new ValidatedFile(bytes, PDF_CONTENT_TYPE, "pdf", originalName(file));
        }
        DetectedImage image = detectImage(bytes, messages.invalidContent());
        if (!image.contentType().equalsIgnoreCase(file.getContentType())) {
            throw new BusinessException(messages.mimeMismatch());
        }
        return new ValidatedFile(bytes, image.contentType(), image.extension(), originalName(file));
    }

    private void validatePresenceSizeAndDeclaredType(
            MultipartFile file,
            long maxSizeBytes,
            Set<String> allowedContentTypes,
            UploadMessages messages
    ) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(messages.required());
        }
        if (!allowedContentTypes.contains(file.getContentType())) {
            throw new BusinessException(messages.invalidType());
        }
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException(messages.tooLarge());
        }
    }

    private DetectedImage detectImage(byte[] bytes, String invalidContentMessage) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new BusinessException(invalidContentMessage);
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new BusinessException(invalidContentMessage);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > maxImagePixels) {
                    throw new BusinessException("Las dimensiones de la imagen no son validas");
                }
                String format = normalizeImageFormat(reader.getFormatName(), invalidContentMessage);
                if (reader.read(0) == null) {
                    throw new BusinessException(invalidContentMessage);
                }
                return switch (format) {
                    case "jpeg" -> new DetectedImage("image/jpeg", "jpg");
                    case "png" -> new DetectedImage("image/png", "png");
                    case "webp" -> new DetectedImage("image/webp", "webp");
                    default -> throw new BusinessException(invalidContentMessage);
                };
            } finally {
                reader.dispose();
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(invalidContentMessage);
        }
    }

    private String normalizeImageFormat(String format, String invalidContentMessage) {
        String normalized = format.toLowerCase(Locale.ROOT);
        if (normalized.equals("jpg") || normalized.equals("jpeg")) return "jpeg";
        if (normalized.equals("png")) return "png";
        if (normalized.equals("webp")) return "webp";
        throw new BusinessException(invalidContentMessage);
    }

    private void validatePdf(byte[] bytes, String invalidContentMessage) {
        byte[] header = "%PDF-".getBytes(StandardCharsets.US_ASCII);
        if (bytes.length < header.length + 5) {
            throw new BusinessException(invalidContentMessage);
        }
        for (int index = 0; index < header.length; index++) {
            if (bytes[index] != header[index]) {
                throw new BusinessException(invalidContentMessage);
            }
        }
        int tailStart = Math.max(0, bytes.length - 2048);
        String tail = new String(bytes, tailStart, bytes.length - tailStart, StandardCharsets.ISO_8859_1);
        if (!tail.contains("%%EOF")) {
            throw new BusinessException(invalidContentMessage);
        }
    }

    private byte[] read(MultipartFile file, String message) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(message);
        }
    }

    private String originalName(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        return originalName == null || originalName.isBlank() ? "archivo" : originalName;
    }

    public record ValidatedFile(byte[] bytes, String contentType, String extension, String originalName) {
    }

    public record UploadMessages(
            String required,
            String invalidType,
            String tooLarge,
            String invalidContent,
            String mimeMismatch,
            String readError
    ) {
    }

    private record DetectedImage(String contentType, String extension) {
    }
}
