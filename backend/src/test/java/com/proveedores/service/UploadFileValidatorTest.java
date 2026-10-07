package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.exception.BusinessException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class UploadFileValidatorTest {

    private static final UploadFileValidator.UploadMessages MESSAGES = new UploadFileValidator.UploadMessages(
            "obligatorio", "tipo invalido", "demasiado grande", "contenido invalido", "mime falso", "error lectura");

    @Test
    void aceptaImagenValidaYDetectaFormatoReal() throws Exception {
        byte[] png = image("png", 4, 3);
        UploadFileValidator validator = new UploadFileValidator(100);

        var result = validator.validateImage(file("foto.png", "image/png", png), 1024 * 1024, MESSAGES);

        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.extension()).isEqualTo("png");
        assertThat(result.bytes()).containsExactly(png);
    }

    @Test
    void rechazaExtensionValidaConContenidoFalso() {
        UploadFileValidator validator = new UploadFileValidator(100);

        assertThatThrownBy(() -> validator.validateImage(
                file("foto.png", "image/png", "no-es-imagen".getBytes(StandardCharsets.UTF_8)), 1024, MESSAGES))
                .isInstanceOf(BusinessException.class)
                .hasMessage("contenido invalido");
    }

    @Test
    void rechazaMimePermitidoConImagenCorrupta() {
        UploadFileValidator validator = new UploadFileValidator(100);
        byte[] corrupt = {(byte) 0x89, 'P', 'N', 'G', 0, 1, 2};

        assertThatThrownBy(() -> validator.validateImage(file("foto.png", "image/png", corrupt), 1024, MESSAGES))
                .isInstanceOf(BusinessException.class)
                .hasMessage("contenido invalido");
    }

    @Test
    void rechazaMimeQueNoCoincideConFormatoDetectado() throws Exception {
        UploadFileValidator validator = new UploadFileValidator(100);

        assertThatThrownBy(() -> validator.validateImage(
                file("foto.jpg", "image/jpeg", image("png", 2, 2)), 1024 * 1024, MESSAGES))
                .isInstanceOf(BusinessException.class)
                .hasMessage("mime falso");
    }

    @Test
    void rechazaArchivoDemasiadoGrandeAntesDeDecodificar() {
        UploadFileValidator validator = new UploadFileValidator(100);

        assertThatThrownBy(() -> validator.validateImage(file("foto.png", "image/png", new byte[20]), 10, MESSAGES))
                .isInstanceOf(BusinessException.class)
                .hasMessage("demasiado grande");
    }

    @Test
    void rechazaImagenConDemasiadosPixeles() throws Exception {
        UploadFileValidator validator = new UploadFileValidator(100);

        assertThatThrownBy(() -> validator.validateImage(
                file("foto.png", "image/png", image("png", 11, 10)), 1024 * 1024, MESSAGES))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Las dimensiones de la imagen no son validas");
    }

    @Test
    void pdfRequiereFirmaYMarcadorFinal() {
        UploadFileValidator validator = new UploadFileValidator(100);
        byte[] validPdf = "%PDF-1.4\n1 0 obj\nendobj\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

        assertThat(validator.validateImageOrPdf(file("recibo.pdf", "application/pdf", validPdf), 1024, MESSAGES).extension())
                .isEqualTo("pdf");
        assertThatThrownBy(() -> validator.validateImageOrPdf(
                file("recibo.pdf", "application/pdf", "%PDF-1.4 sin cierre".getBytes(StandardCharsets.US_ASCII)),
                1024,
                MESSAGES
        )).isInstanceOf(BusinessException.class).hasMessage("contenido invalido");
    }

    private MockMultipartFile file(String name, String contentType, byte[] bytes) {
        return new MockMultipartFile("archivo", name, contentType, bytes);
    }

    private byte[] image(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
