package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.proveedores.dto.CategoriaObjetoResponseDTO;
import com.proveedores.dto.ExhibicionObjetoResponseDTO;
import com.proveedores.dto.FotoObjetoMuseoResponseDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.dto.ReciboIngresoObjetoResponseDTO;
import com.proveedores.dto.RelacionElementoResponseDTO;
import com.proveedores.dto.TipoNodoRelacion;
import com.proveedores.dto.TipoVinculoRelacion;
import com.proveedores.entity.EstadoExhibicionObjeto;
import com.proveedores.entity.VisibilidadCampo;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.report.PdfReportService;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ObjetoMuseoFichaPdfServiceTest {

    @Mock private ObjetoMuseoService objetoMuseoService;
    @Mock private FotoObjetoMuseoService fotoObjetoMuseoService;
    @Mock private RelacionObjetoService relacionObjetoService;
    @Mock private ExhibicionObjetoService exhibicionObjetoService;
    @Mock private ReciboIngresoObjetoService reciboIngresoObjetoService;
    @Mock private DetalleConservacionService detalleConservacionService;

    private ObjetoMuseoFichaPdfService service;

    @BeforeEach
    void setUp() {
        service = new ObjetoMuseoFichaPdfService(objetoMuseoService, fotoObjetoMuseoService,
                relacionObjetoService, exhibicionObjetoService, reciboIngresoObjetoService,
                detalleConservacionService, new PdfReportService());
        lenient().when(relacionObjetoService.listarPorObjeto(1L)).thenReturn(List.of());
        lenient().when(exhibicionObjetoService.listarPorObjeto(1L)).thenReturn(List.of());
        lenient().when(detalleConservacionService.listar()).thenReturn(List.of());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generaPdfValidoConTodasLasSeccionesRelacionesExhibicionesYRecibosParaAdmin() throws Exception {
        authenticate("ADMIN");
        when(objetoMuseoService.obtenerPorId(1L)).thenReturn(object("Descripción pública con ñ y áéíóú", List.of()));
        when(relacionObjetoService.listarPorObjeto(1L)).thenReturn(List.of(new RelacionElementoResponseDTO(
                "r1", TipoVinculoRelacion.OBJETO_PERSONA, TipoNodoRelacion.PERSONA, 8L,
                null, "José Pérez", "DONANTE", "Relación documentada", "SALIENTE")));
        when(exhibicionObjetoService.listarPorObjeto(1L)).thenReturn(List.of(new ExhibicionObjetoResponseDTO(
                9L, 3L, "Memoria de Malvinas", 1L, "MM-001", "Casco",
                LocalDate.of(2026, 1, 1), null, EstadoExhibicionObjeto.EN_EXHIBICION,
                false, null, null, null, null)));
        when(reciboIngresoObjetoService.listarPorObjeto(1L)).thenReturn(List.of(new ReciboIngresoObjetoResponseDTO(
                4L, "REC-004", LocalDateTime.of(2026, 1, 2, 10, 30), 1L, 2L,
                "MM-001", "Casco", "Descripción", "Depositante", null, "admin",
                "Constancia", true, "recibo-firmado.pdf", LocalDateTime.now(), "admin")));

        ObjetoMuseoFichaPdfService.FichaPdf result = service.generar(1L, "admin");
        String text = pdfText(result.contenido());

        assertThat(result.contenido()).startsWith("%PDF".getBytes());
        assertThat(result.nombreArchivo()).isEqualTo("ficha-objeto-MM-001.pdf");
        assertThat(text).contains("FICHA COMPLETA DEL OBJETO", "Datos generales", "Información técnica",
                "Situación legal", "Estado de conservación", "Conservación preventiva", "Inventario",
                "Relaciones", "José Pérez", "Exhibiciones", "Memoria de Malvinas", "Recibos", "REC-004",
                "Descripción pública con ñ y áéíóú", "Sin fotografías visibles");
    }

    @Test
    void viewerRecibeSoloDtoFiltradoFotosAutorizadasYSinRecibosPrivados() throws Exception {
        authenticate("VIEWER");
        FotoObjetoMuseoResponseDTO publicPhoto = photo(11L, "Foto pública");
        ObjetoMuseoResponseDTO filtered = object("Texto público", List.of(publicPhoto));
        when(objetoMuseoService.obtenerPorId(1L)).thenReturn(filtered);
        when(fotoObjetoMuseoService.descargar(1L, 11L)).thenReturn(new FotoObjetoMuseoService.FotoArchivo(
                publicPhoto, new ByteArrayResource(imageBytes(2400, 1200)), "image/png", false, 60));

        byte[] pdf = service.generar(1L, "viewer").contenido();
        String text = pdfText(pdf);

        assertThat(text).contains("Texto público", "Foto pública", "Fotografía principal");
        assertThat(text).doesNotContain("RECIBO PRIVADO", "VIDEO SECRETO");
        verify(reciboIngresoObjetoService, never()).listarPorObjeto(anyLong());
        verify(fotoObjetoMuseoService).descargar(1L, 11L);
        verify(fotoObjetoMuseoService, never()).descargarOriginal(anyLong(), anyLong());
    }

    @Test
    void museologoIncluyeRecibosAutorizados() throws Exception {
        authenticate("MUSEOLOGO");
        when(objetoMuseoService.obtenerPorId(1L)).thenReturn(object(null, List.of()));
        when(reciboIngresoObjetoService.listarPorObjeto(1L)).thenReturn(List.of(new ReciboIngresoObjetoResponseDTO(
                5L, "REC-MUS", null, 1L, null, null, null, null, null, null,
                null, null, false, null, null, null)));

        assertThat(pdfText(service.generar(1L, "museologo").contenido())).contains("REC-MUS");
    }

    @Test
    void continuaConMultiplesFotosAunqueUnaNoPuedaProcesarseYNoIncluyeVideos() throws Exception {
        authenticate("ADMIN");
        List<FotoObjetoMuseoResponseDTO> photos = List.of(photo(11L, "Primera"), photo(12L, "Segunda"), photo(13L, "Tercera"));
        when(objetoMuseoService.obtenerPorId(1L)).thenReturn(object("Sin contenido VIDEO", photos));
        when(fotoObjetoMuseoService.descargar(1L, 11L)).thenReturn(file(photos.get(0), imageBytes(100, 180)));
        when(fotoObjetoMuseoService.descargar(1L, 12L)).thenReturn(file(photos.get(1), "no-image".getBytes()));
        when(fotoObjetoMuseoService.descargar(1L, 13L)).thenReturn(file(photos.get(2), imageBytes(180, 100)));

        String text = pdfText(service.generar(1L, "admin").contenido());

        assertThat(text).contains("Primera", "Segunda", "Tercera", "Imagen no disponible");
        assertThat(text).doesNotContain("reproductor", "código QR");
    }

    @Test
    void admiteTextoExtensoVariasPaginasYNoGeneraPaginasVacias() throws Exception {
        authenticate("ADMIN");
        when(objetoMuseoService.obtenerPorId(1L)).thenReturn(object("Línea extensa con acentos.\n".repeat(400), List.of()));

        byte[] bytes = service.generar(1L, "admin").contenido();
        try (PdfDocument pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(bytes)))) {
            assertThat(pdf.getNumberOfPages()).isGreaterThan(1);
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                assertThat(PdfTextExtractor.getTextFromPage(pdf.getPage(page))).isNotBlank();
            }
        }
    }

    @Test
    void conservaPoliticaDeObjetoInexistenteOEmbargado() {
        authenticate("VIEWER");
        when(objetoMuseoService.obtenerPorId(1L)).thenThrow(new ResourceNotFoundException("Objeto no encontrado"));

        assertThatThrownBy(() -> service.generar(1L, "viewer"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Objeto no encontrado");
        verify(relacionObjetoService, never()).listarPorObjeto(anyLong());
    }

    private ObjetoMuseoResponseDTO object(String description, List<FotoObjetoMuseoResponseDTO> photos) {
        return new ObjetoMuseoResponseDTO(
                1L, "MM-001", "Casco de combate", description, "Descripción técnica completa",
                "Acero y cuero", "25 cm", "20 cm", "18 cm", "2 mm", "1,2 kg",
                "Inscripción áéíóú", null, "Bien patrimonial", null, Set.of("BUEN_ESTADO"),
                null, null, null, "18 °C", "50 lux", true, false, true, true, false,
                Map.of(), LocalDate.of(2025, 5, 2), null, true, null, null,
                2L, "Depósito A", true, 3L, "Colección histórica", 4L, "Juan Pérez",
                null, null, List.of(new CategoriaObjetoResponseDTO(1L, "Indumentaria", null)),
                photos, null, "25 × 20 cm", 2);
    }

    private FotoObjetoMuseoResponseDTO photo(Long id, String description) {
        return new FotoObjetoMuseoResponseDTO(id, 1L, "foto-" + id + ".png", "interno-" + id,
                "image/png", 100L, description, VisibilidadCampo.PUBLICO,
                LocalDateTime.of(2026, 1, 1, 10, id.intValue() % 50), "admin");
    }

    private FotoObjetoMuseoService.FotoArchivo file(FotoObjetoMuseoResponseDTO photo, byte[] bytes) {
        return new FotoObjetoMuseoService.FotoArchivo(photo, new ByteArrayResource(bytes), "image/png", false, 60);
    }

    private byte[] imageBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private String pdfText(byte[] bytes) throws Exception {
        try (PdfDocument pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(bytes)))) {
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                text.append(PdfTextExtractor.getTextFromPage(pdf.getPage(page)));
            }
            return text.toString();
        }
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                role.toLowerCase(), "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
