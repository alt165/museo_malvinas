package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.config.ImageProtectionProperties;
import com.proveedores.exception.BusinessException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ImageWatermarkServiceTest {

    private ImageWatermarkService service;

    @BeforeEach
    void setUp() {
        service = new ImageWatermarkService(new ImageProtectionProperties(
                new ImageProtectionProperties.PublicVersion(1600, 86400),
                new ImageProtectionProperties.Watermark(
                        0.22f, 0.18, 0.35, 0.45,
                        new ClassPathResource("watermark/logo-header.png"))));
    }

    @Test
    void reduceImagenGrandeManteniendoRelacionDeAspectoYMarcaRepetida() throws Exception {
        byte[] source = imagenPng(2400, 1200, Color.WHITE);

        var generated = service.generar(source, "image/png");
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(generated.bytes()));

        assertThat(result.getWidth()).isEqualTo(1600);
        assertThat(result.getHeight()).isEqualTo(800);
        assertThat(generated.contentType()).isEqualTo("image/png");
        assertThat(cantidadPixelesModificados(result, 0, 0, 800, 400)).isGreaterThan(100);
        assertThat(cantidadPixelesModificados(result, 800, 0, 800, 400)).isGreaterThan(100);
        assertThat(cantidadPixelesModificados(result, 0, 400, 800, 400)).isGreaterThan(100);
        assertThat(cantidadPixelesModificados(result, 800, 400, 800, 400)).isGreaterThan(100);
    }

    @Test
    void noAmpliaImagenPequena() throws Exception {
        var generated = service.generar(imagenPng(640, 480, Color.WHITE), "image/png");
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(generated.bytes()));

        assertThat(result.getWidth()).isEqualTo(640);
        assertThat(result.getHeight()).isEqualTo(480);
    }

    @Test
    void rechazaContenidoInvalidoYMimeFalso() throws Exception {
        assertThatThrownBy(() -> service.generar("no es imagen".getBytes(), "image/png"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.generar(imagenPng(100, 100, Color.WHITE), "image/jpeg"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El contenido de la imagen no coincide con su tipo MIME");
    }

    private byte[] imagenPng(int width, int height, Color color) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private long cantidadPixelesModificados(BufferedImage image, int startX, int startY, int width, int height) {
        long count = 0;
        for (int y = startY; y < startY + height; y += 3) {
            for (int x = startX; x < startX + width; x += 3) {
                if ((image.getRGB(x, y) & 0x00FFFFFF) != 0x00FFFFFF) count++;
            }
        }
        return count;
    }
}
