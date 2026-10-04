package com.proveedores.service;

import com.proveedores.config.ImageProtectionProperties;
import com.proveedores.exception.BusinessException;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.stereotype.Service;

@Service
public class ImageWatermarkService {

    private static final long MAX_PIXELS = 100_000_000L;
    private final ImageProtectionProperties properties;
    private final BufferedImage watermark;

    public ImageWatermarkService(ImageProtectionProperties properties) {
        this.properties = properties;
        this.watermark = cargarMarca(properties);
    }

    public GeneratedPublicImage generar(byte[] original, String declaredContentType) {
        DecodedImage decoded = decodificar(original, declaredContentType);
        BufferedImage resized = redimensionar(decoded.image());
        aplicarMarca(resized);
        String outputFormat = decoded.format().equals("jpeg") ? "jpeg" : "png";
        String contentType = outputFormat.equals("jpeg") ? "image/jpeg" : "image/png";
        byte[] bytes = codificar(resized, outputFormat);
        return new GeneratedPublicImage(bytes, contentType, outputFormat.equals("jpeg") ? "jpg" : "png", resized.getWidth(), resized.getHeight());
    }

    public long publicCacheMaxAgeSeconds() {
        return properties.publicVersion().cacheMaxAgeSeconds();
    }

    private DecodedImage decodificar(byte[] bytes, String declaredContentType) {
        if (bytes == null || bytes.length == 0) throw new BusinessException("La foto es obligatoria");
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new BusinessException("El archivo no contiene una imagen valida");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw new BusinessException("Las dimensiones de la imagen no son validas");
                }
                String format = normalizarFormato(reader.getFormatName());
                validarMime(format, declaredContentType);
                BufferedImage image = reader.read(0);
                if (image == null) throw new BusinessException("El archivo no contiene una imagen valida");
                return new DecodedImage(image, format);
            } finally {
                reader.dispose();
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new BusinessException("No se pudo procesar la imagen");
        }
    }

    private String normalizarFormato(String format) {
        String normalized = format.toLowerCase(Locale.ROOT);
        if (normalized.equals("jpg") || normalized.equals("jpeg")) return "jpeg";
        if (normalized.equals("png")) return "png";
        if (normalized.equals("webp")) return "webp";
        throw new BusinessException("Tipo de imagen no permitido");
    }

    private void validarMime(String format, String declaredContentType) {
        String expected = switch (format) {
            case "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> throw new BusinessException("Tipo de imagen no permitido");
        };
        if (!expected.equalsIgnoreCase(declaredContentType)) {
            throw new BusinessException("El contenido de la imagen no coincide con su tipo MIME");
        }
    }

    private BufferedImage redimensionar(BufferedImage source) {
        int maxSize = properties.publicVersion().maxSize();
        int largest = Math.max(source.getWidth(), source.getHeight());
        double scale = largest > maxSize ? (double) maxSize / largest : 1.0;
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int type = source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage result = new BufferedImage(width, height, type);
        Graphics2D graphics = result.createGraphics();
        try {
            aplicarCalidad(graphics);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private void aplicarMarca(BufferedImage image) {
        int logoWidth = Math.max(1, (int) Math.round(image.getWidth() * properties.watermark().relativeWidth()));
        int logoHeight = Math.max(1, (int) Math.round((double) watermark.getHeight() * logoWidth / watermark.getWidth()));
        int gapX = Math.max(1, (int) Math.round(logoWidth * properties.watermark().horizontalGap()));
        int gapY = Math.max(1, (int) Math.round(logoHeight * properties.watermark().verticalGap()));
        int stepX = logoWidth + gapX;
        int stepY = logoHeight + gapY;
        Graphics2D graphics = image.createGraphics();
        try {
            aplicarCalidad(graphics);
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, properties.watermark().opacity()));
            int row = 0;
            for (int y = -logoHeight / 2; y < image.getHeight(); y += stepY, row++) {
                int offset = row % 2 == 0 ? 0 : -(stepX / 2);
                for (int x = offset; x < image.getWidth(); x += stepX) {
                    graphics.drawImage(watermark, x, y, logoWidth, logoHeight, null);
                }
            }
        } finally {
            graphics.dispose();
        }
    }

    private void aplicarCalidad(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private byte[] codificar(BufferedImage image, String format) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
            if (!writers.hasNext()) throw new BusinessException("No se pudo codificar la imagen publica");
            ImageWriter writer = writers.next();
            try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
                writer.setOutput(imageOutput);
                ImageWriteParam params = writer.getDefaultWriteParam();
                if (format.equals("jpeg") && params.canWriteCompressed()) {
                    params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    params.setCompressionQuality(0.9f);
                }
                writer.write(null, new IIOImage(image, null, null), params);
            } finally {
                writer.dispose();
            }
            return output.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("No se pudo generar la imagen publica");
        }
    }

    private BufferedImage cargarMarca(ImageProtectionProperties properties) {
        try (InputStream input = properties.watermark().resource().getInputStream()) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) throw new BusinessException("No se pudo leer la marca de agua configurada");
            return image;
        } catch (IOException ex) {
            throw new BusinessException("No se pudo cargar la marca de agua configurada");
        }
    }

    private record DecodedImage(BufferedImage image, String format) {
    }

    public record GeneratedPublicImage(byte[] bytes, String contentType, String extension, int width, int height) {
    }
}
