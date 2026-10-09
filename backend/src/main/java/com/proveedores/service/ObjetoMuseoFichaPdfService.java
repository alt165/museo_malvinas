package com.proveedores.service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.proveedores.dto.CategoriaObjetoResponseDTO;
import com.proveedores.dto.DetalleConservacionResponseDTO;
import com.proveedores.dto.ExhibicionObjetoResponseDTO;
import com.proveedores.dto.FotoObjetoMuseoResponseDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.dto.ReciboIngresoObjetoResponseDTO;
import com.proveedores.dto.RelacionElementoResponseDTO;
import com.proveedores.report.PdfReportService;
import com.proveedores.report.PdfReportService.DetailedReportContext;
import com.proveedores.report.ReportMetadata;
import com.proveedores.security.ObjetoVisibilityPolicy;
import com.proveedores.time.MuseoTime;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ObjetoMuseoFichaPdfService {

    private static final Logger log = LoggerFactory.getLogger(ObjetoMuseoFichaPdfService.class);
    private static final String INSTITUTION = "Museo de la Guerra de Malvinas";
    private static final String TITLE = "FICHA COMPLETA DEL OBJETO";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int MAX_SOURCE_IMAGE_BYTES = 15 * 1024 * 1024;
    private static final int MAX_IMAGE_EDGE = 1200;

    private final ObjetoMuseoService objetoMuseoService;
    private final FotoObjetoMuseoService fotoObjetoMuseoService;
    private final RelacionObjetoService relacionObjetoService;
    private final ExhibicionObjetoService exhibicionObjetoService;
    private final ReciboIngresoObjetoService reciboIngresoObjetoService;
    private final DetalleConservacionService detalleConservacionService;
    private final PdfReportService pdfReportService;

    public ObjetoMuseoFichaPdfService(
            ObjetoMuseoService objetoMuseoService,
            FotoObjetoMuseoService fotoObjetoMuseoService,
            RelacionObjetoService relacionObjetoService,
            ExhibicionObjetoService exhibicionObjetoService,
            ReciboIngresoObjetoService reciboIngresoObjetoService,
            DetalleConservacionService detalleConservacionService,
            PdfReportService pdfReportService
    ) {
        this.objetoMuseoService = objetoMuseoService;
        this.fotoObjetoMuseoService = fotoObjetoMuseoService;
        this.relacionObjetoService = relacionObjetoService;
        this.exhibicionObjetoService = exhibicionObjetoService;
        this.reciboIngresoObjetoService = reciboIngresoObjetoService;
        this.detalleConservacionService = detalleConservacionService;
        this.pdfReportService = pdfReportService;
    }

    public FichaPdf generar(Long objetoId, String usuario) {
        // Este DTO ya aplica embargo y visibilidad de campos para el usuario autenticado.
        ObjetoMuseoResponseDTO objeto = objetoMuseoService.obtenerPorId(objetoId);
        List<RelacionElementoResponseDTO> relaciones = relacionObjetoService.listarPorObjeto(objetoId);
        List<ExhibicionObjetoResponseDTO> exhibiciones = exhibicionObjetoService.listarPorObjeto(objetoId);
        List<ReciboIngresoObjetoResponseDTO> recibos = ObjetoVisibilityPolicy.puedeVerCamposPrivados()
                ? reciboIngresoObjetoService.listarPorObjeto(objetoId)
                : List.of();
        Map<String, String> detallesConservacion = detallesConservacionService();
        List<FotoObjetoMuseoResponseDTO> fotos = ordenarFotos(objeto.fotos());
        LocalDateTime generatedAt = MuseoTime.now();

        byte[] contenido = pdfReportService.generatePortrait(
                TITLE,
                new ReportMetadata(INSTITUTION, generatedAt, usuario),
                context -> construirDocumento(context, objeto, relaciones, exhibiciones, recibos,
                        detallesConservacion, fotos, usuario, generatedAt));
        return new FichaPdf(contenido, nombreArchivo(objeto));
    }

    private void construirDocumento(
            DetailedReportContext context,
            ObjetoMuseoResponseDTO objeto,
            List<RelacionElementoResponseDTO> relaciones,
            List<ExhibicionObjetoResponseDTO> exhibiciones,
            List<ReciboIngresoObjetoResponseDTO> recibos,
            Map<String, String> detallesConservacion,
            List<FotoObjetoMuseoResponseDTO> fotos,
            String usuario,
            LocalDateTime generatedAt
    ) {
        String denominacion = valueOr(objeto.denominacionObjeto(), "Objeto patrimonial");
        context.document().add(new Paragraph(denominacion)
                .setFont(context.boldFont()).setFontSize(17).setFontColor(context.primaryColor())
                .setMarginBottom(2));
        if (hasText(objeto.numeroInventario())) {
            context.document().add(new Paragraph("N.º de inventario: " + objeto.numeroInventario())
                    .setFont(context.boldFont()).setFontSize(10).setMarginBottom(2));
        }
        context.document().add(new Paragraph("Generado el " + DATE_TIME.format(generatedAt)
                + " por " + valueOr(usuario, "usuario autenticado"))
                .setFontSize(8).setFontColor(ColorConstants.DARK_GRAY).setMarginBottom(12));

        addSection(context, "Datos generales", fields(
                field("Número de inventario", objeto.numeroInventario()),
                field("Denominación", objeto.denominacionObjeto()),
                field("Categorías", joinCategories(objeto.categorias())),
                field("Colección", objeto.coleccionNombre()),
                field("Descripción", objeto.descripcion())
        ));
        addSection(context, "Recepción y procedencia", fields(
                field("Depositante", objeto.depositanteNombre()),
                field("Carácter de recepción", label(objeto.caracterRecepcion())),
                field("Fecha de ingreso", date(objeto.fechaIngreso())),
                field("Vencimiento de préstamo o comodato", date(objeto.fechaVencimiento()))
        ));
        addSection(context, "Información técnica", fields(
                field("Materiales", objeto.materiales()),
                field("Alto", objeto.alto()),
                field("Ancho", objeto.ancho()),
                field("Diámetro", objeto.diametro()),
                field("Espesor", objeto.espesor()),
                field("Peso", objeto.peso()),
                field("Medidas", objeto.medidas()),
                field("Partes", partNumbers(objeto.numeroInventario(), objeto.cantidadPartes())),
                field("Inscripciones", objeto.inscripciones()),
                field("Descripción técnica", objeto.descripcionTecnica())
        ));
        addSection(context, "Situación legal", fields(
                field("Régimen de propiedad", label(objeto.regimenPropiedad())),
                field("Condición legal del bien", objeto.condicionLegalBien())
        ));
        addSection(context, "Estado de conservación", fields(
                field("Estado de conservación", label(objeto.estadoConservacion())),
                field("Detalles de conservación", joinConservationDetails(objeto.detallesEstadoConservacion(), detallesConservacion)),
                field("Intervenciones inadecuadas", label(objeto.intervencionesInadecuadas())),
                field("Estado de integridad", label(objeto.estadoIntegridad()))
        ));
        addSection(context, "Conservación preventiva", fields(
                field("Humedad", label(objeto.humedadConservacion())),
                field("Temperatura", objeto.temperaturaConservacion()),
                field("Iluminación", objeto.luzConservacion()),
                field("Extintores", booleanLabel(objeto.conservacionExtintores())),
                field("Montaje", booleanLabel(objeto.conservacionMontaje())),
                field("Sistema eléctrico", booleanLabel(objeto.conservacionSistemaElectrico())),
                field("Alarmas", booleanLabel(objeto.conservacionAlarmas())),
                field("Cámaras", booleanLabel(objeto.conservacionCamaras()))
        ));
        addSection(context, "Inventario", fields(
                field("Ubicación actual", Boolean.FALSE.equals(objeto.ubicacionVisible()) ? null : objeto.ubicacionNombre())
        ));
        addRelations(context, relaciones);
        addExhibitions(context, exhibiciones);
        addReceipts(context, recibos);
        addPhotographs(context, objeto.id(), fotos);
    }

    private void addSection(DetailedReportContext context, String title, List<FieldValue> values) {
        if (values.isEmpty()) return;
        addSectionTitle(context, title);
        Table table = new Table(UnitValue.createPercentArray(new float[]{31, 69})).useAllAvailableWidth();
        table.setFontSize(9);
        for (FieldValue value : values) {
            table.addCell(baseCell(context).setBackgroundColor(context.headerBackgroundColor())
                    .add(new Paragraph(value.label()).setFont(context.boldFont())));
            table.addCell(baseCell(context)
                    .add(new Paragraph(value.value()).setMultipliedLeading(1.2f)));
        }
        table.setMarginBottom(9);
        context.document().add(table);
    }

    private void addRelations(DetailedReportContext context, List<RelacionElementoResponseDTO> relaciones) {
        if (relaciones == null || relaciones.isEmpty()) return;
        addSectionTitle(context, "Relaciones");
        Table table = titledTable(context, List.of("Elemento", "Tipo de relación", "Descripción"), new float[]{35, 25, 40});
        for (RelacionElementoResponseDTO relacion : relaciones) {
            String elemento = label(relacion.tipoElemento()) + ": " + valueOr(relacion.denominacion(), "Sin denominación");
            if (hasText(relacion.numeroInventario())) elemento += " (" + relacion.numeroInventario() + ")";
            addRow(context, table, elemento, relacion.tipoRelacion(), relacion.descripcion());
        }
        table.setMarginBottom(9);
        context.document().add(table);
    }

    private void addExhibitions(DetailedReportContext context, List<ExhibicionObjetoResponseDTO> exhibiciones) {
        if (exhibiciones == null || exhibiciones.isEmpty()) return;
        addSectionTitle(context, "Exhibiciones");
        Table table = titledTable(context,
                List.of("Exhibición", "Inclusión", "Retiro", "Estado", "Devolución"),
                new float[]{31, 16, 16, 19, 18});
        for (ExhibicionObjetoResponseDTO exhibicion : exhibiciones) {
            addRow(context, table, exhibicion.exhibicionNombre(), date(exhibicion.fechaInclusion()),
                    date(exhibicion.fechaRetiro()), label(exhibicion.estado()),
                    Boolean.TRUE.equals(exhibicion.devolucionVerificada()) ? "Verificada" : "Pendiente");
            if (hasText(exhibicion.observacionesDevolucion())) {
                table.addCell(new Cell(1, 5).add(new Paragraph("Observaciones: " + exhibicion.observacionesDevolucion()))
                        .setPadding(5).setBorder(new SolidBorder(context.borderColor(), 0.4f)));
            }
        }
        table.setMarginBottom(9);
        context.document().add(table);
    }

    private void addReceipts(DetailedReportContext context, List<ReciboIngresoObjetoResponseDTO> recibos) {
        if (recibos == null || recibos.isEmpty()) return;
        addSectionTitle(context, "Recibos");
        Table table = titledTable(context, List.of("Número", "Copia firmada", "Archivo"),
                new float[]{25, 25, 50});
        for (ReciboIngresoObjetoResponseDTO recibo : recibos) {
            addRow(context, table, recibo.numeroRecibo(),
                    Boolean.TRUE.equals(recibo.tieneCopiaFirmada()) ? "Sí" : "No",
                    recibo.copiaFirmadaNombreArchivo());
        }
        table.setMarginBottom(9);
        context.document().add(table);
    }

    private void addPhotographs(DetailedReportContext context, Long objetoId, List<FotoObjetoMuseoResponseDTO> fotos) {
        addSectionTitle(context, "Registro fotográfico");
        if (fotos.isEmpty()) {
            context.document().add(new Paragraph("Sin fotografías visibles.").setFontSize(9).setMarginBottom(8));
            return;
        }

        addPhotoBlock(context, objetoId, fotos.get(0), true);
        if (fotos.size() == 1) return;

        Table grid = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
        grid.setMarginTop(7).setMarginBottom(8);
        for (int index = 1; index < fotos.size(); index++) {
            grid.addCell(photoCell(context, objetoId, fotos.get(index), false));
        }
        if ((fotos.size() - 1) % 2 != 0) {
            grid.addCell(new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        }
        context.document().add(grid);
    }

    private void addPhotoBlock(DetailedReportContext context, Long objetoId, FotoObjetoMuseoResponseDTO foto, boolean principal) {
        Cell container = photoCell(context, objetoId, foto, principal);
        Table table = new Table(1).useAllAvailableWidth().setMarginBottom(5);
        table.addCell(container);
        context.document().add(table);
    }

    private Cell photoCell(DetailedReportContext context, Long objetoId, FotoObjetoMuseoResponseDTO foto, boolean principal) {
        Cell cell = new Cell().setPadding(7).setVerticalAlignment(VerticalAlignment.TOP)
                .setBorder(new SolidBorder(context.borderColor(), 0.5f));
        try {
            FotoObjetoMuseoService.FotoArchivo archivo = fotoObjetoMuseoService.descargar(objetoId, foto.id());
            byte[] imageBytes = reducedImage(archivo.resource().getInputStream());
            Image image = new Image(ImageDataFactory.create(imageBytes));
            image.setAutoScale(true);
            image.setMaxHeight(principal ? 310 : 185);
            image.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            cell.add(image);
            if (principal) {
                cell.add(new Paragraph("Fotografía principal").setFont(context.boldFont())
                        .setTextAlignment(TextAlignment.CENTER).setFontSize(8).setMarginTop(4).setMarginBottom(0));
            }
            if (hasText(foto.descripcion())) {
                cell.add(new Paragraph(foto.descripcion()).setFontSize(8)
                        .setTextAlignment(TextAlignment.CENTER).setMarginTop(3).setMarginBottom(0));
            }
        } catch (Exception exception) {
            log.warn("No se pudo incorporar una fotografía al PDF. objetoId={} fotoId={} tipoError={}",
                    objetoId, foto.id(), exception.getClass().getSimpleName());
            cell.add(new Paragraph("Imagen no disponible").setFontSize(8)
                    .setFontColor(ColorConstants.DARK_GRAY).setTextAlignment(TextAlignment.CENTER));
            if (hasText(foto.descripcion())) {
                cell.add(new Paragraph(foto.descripcion()).setFontSize(8)
                        .setTextAlignment(TextAlignment.CENTER).setMarginTop(3).setMarginBottom(0));
            }
        }
        return cell;
    }

    private byte[] reducedImage(InputStream input) throws IOException {
        try (input) {
            byte[] source = input.readNBytes(MAX_SOURCE_IMAGE_BYTES + 1);
            if (source.length > MAX_SOURCE_IMAGE_BYTES) throw new IOException("Imagen excede el límite de procesamiento");
            BufferedImage original = ImageIO.read(new java.io.ByteArrayInputStream(source));
            if (original == null) throw new IOException("Formato de imagen no reconocido");
            double scale = Math.min(1d, (double) MAX_IMAGE_EDGE / Math.max(original.getWidth(), original.getHeight()));
            int width = Math.max(1, (int) Math.round(original.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(original.getHeight() * scale));
            BufferedImage reduced = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = reduced.createGraphics();
            try {
                graphics.setColor(java.awt.Color.WHITE);
                graphics.fillRect(0, 0, width, height);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.drawImage(original, 0, 0, width, height, null);
            } finally {
                graphics.dispose();
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(reduced, "jpeg", output)) throw new IOException("No se pudo codificar la imagen");
            return output.toByteArray();
        }
    }

    private void addSectionTitle(DetailedReportContext context, String title) {
        context.document().add(new Paragraph(title)
                .setFont(context.boldFont()).setFontSize(11).setFontColor(context.primaryColor())
                .setBorderBottom(new SolidBorder(context.primaryColor(), 0.8f))
                .setPaddingBottom(3).setMarginTop(5).setMarginBottom(5));
    }

    private Table titledTable(DetailedReportContext context, List<String> headers, float[] widths) {
        Table table = new Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth().setFontSize(8);
        for (String header : headers) {
            table.addHeaderCell(baseCell(context).setBackgroundColor(context.primaryColor())
                    .setFontColor(ColorConstants.WHITE).add(new Paragraph(header).setFont(context.boldFont())));
        }
        return table;
    }

    private void addRow(DetailedReportContext context, Table table, String... values) {
        for (String value : values) {
            table.addCell(baseCell(context).add(new Paragraph(valueOr(value, "-"))));
        }
    }

    private Cell baseCell(DetailedReportContext context) {
        return new Cell().setPadding(5).setVerticalAlignment(VerticalAlignment.TOP)
                .setBorder(new SolidBorder(context.borderColor(), 0.4f));
    }

    private Map<String, String> detallesConservacionService() {
        return detalleConservacionService.listar().stream().collect(Collectors.toMap(
                DetalleConservacionResponseDTO::codigo,
                DetalleConservacionResponseDTO::nombre,
                (first, ignored) -> first,
                LinkedHashMap::new));
    }

    private String joinConservationDetails(Set<String> codes, Map<String, String> labels) {
        if (codes == null || codes.isEmpty()) return null;
        return codes.stream().map(code -> labels.getOrDefault(code, labelText(code))).collect(Collectors.joining(", "));
    }

    private List<FotoObjetoMuseoResponseDTO> ordenarFotos(List<FotoObjetoMuseoResponseDTO> fotos) {
        if (fotos == null) return List.of();
        return fotos.stream().sorted(Comparator
                .comparing(FotoObjetoMuseoResponseDTO::fechaCarga, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(FotoObjetoMuseoResponseDTO::id, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<FieldValue> fields(FieldValue... values) {
        List<FieldValue> result = new ArrayList<>();
        for (FieldValue value : values) if (value != null && hasText(value.value())) result.add(value);
        return result;
    }

    private FieldValue field(String label, String value) {
        return hasText(value) ? new FieldValue(label, value) : null;
    }

    private String joinCategories(List<CategoriaObjetoResponseDTO> categorias) {
        if (categorias == null) return null;
        return categorias.stream().map(CategoriaObjetoResponseDTO::nombre).filter(this::hasText).collect(Collectors.joining(", "));
    }

    private String partNumbers(String inventoryNumber, Integer quantity) {
        if (!hasText(inventoryNumber) || quantity == null || quantity <= 0) return null;
        List<String> parts = new ArrayList<>(quantity);
        for (int index = 1; index <= quantity; index++) parts.add(inventoryNumber + "_PT." + index);
        return String.join("\n", parts);
    }

    private String booleanLabel(Boolean value) {
        return value == null ? null : value ? "Sí" : "No";
    }

    private String date(LocalDate value) {
        return value == null ? null : DATE.format(value);
    }

    private String label(Enum<?> value) {
        return value == null ? null : labelText(value.name());
    }

    private String labelText(String value) {
        if (!hasText(value)) return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }

    private String nombreArchivo(ObjetoMuseoResponseDTO objeto) {
        String identifier = hasText(objeto.numeroInventario()) ? objeto.numeroInventario() : String.valueOf(objeto.id());
        String ascii = Normalizer.normalize(identifier, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String safe = ascii.replaceAll("[^A-Za-z0-9._-]+", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
        if (!hasText(safe)) safe = String.valueOf(objeto.id());
        return "ficha-objeto-" + safe + ".pdf";
    }

    private String valueOr(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    private record FieldValue(String label, String value) {
    }

    public record FichaPdf(byte[] contenido, String nombreArchivo) {
    }
}
