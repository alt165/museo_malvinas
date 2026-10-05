package com.proveedores.service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.ObjetoDepositante;
import com.proveedores.entity.ReciboIngresoObjeto;
import com.proveedores.repository.ObjetoDepositanteRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ReciboPdfService {

    private static final String INSTITUCION = "Museo Malvinas, Antártida y Atlántico Sur";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_FECHA_VENCIMIENTO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color PRIMARIO = new DeviceRgb(22, 58, 97);
    private static final Color SECUNDARIO = new DeviceRgb(98, 116, 138);
    private static final Color ACENTO = new DeviceRgb(219, 176, 96);

    private final ObjetoDepositanteRepository objetoDepositanteRepository;

    public ReciboPdfService(ObjetoDepositanteRepository objetoDepositanteRepository) {
        this.objetoDepositanteRepository = objetoDepositanteRepository;
    }

    public byte[] generar(ReciboIngresoObjeto recibo) {
        ObjetoDepositante relacion = objetoDepositanteRepository
                .findRelacionActivaPorObjeto(recibo.getObjetoMuseo().getId())
                .orElse(null);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(salida);
                PdfDocument pdf = new PdfDocument(writer);
                Document documento = new Document(pdf, PageSize.A4)) {
            documento.setMargins(42, 48, 42, 48);
            agregarEncabezado(documento);
            agregarIdentificacion(documento, recibo);
            agregarSeccion(documento, "DATOS DEL DEPOSITANTE");
            List<String[]> datosDepositante = new ArrayList<>();
            datosDepositante.add(new String[]{"Depositante", recibo.getDepositanteNombre()});
            agregarDatoSiExiste(datosDepositante, "Email", recibo.getDepositanteContacto());
            String telefono = relacion == null || relacion.getDepositante() == null
                    ? null
                    : telefonoDesdeObservaciones(relacion.getDepositante().getObservaciones());
            agregarDatoSiExiste(datosDepositante, "Teléfono", telefono);
            agregarDatos(documento, datosDepositante.toArray(String[][]::new));
            agregarSeccion(documento, "DATOS DEL OBJETO");
            List<String[]> datosObjeto = new ArrayList<>(List.of(
                    new String[]{"Número de inventario", recibo.getNumeroInventario()},
                    new String[]{"Objeto", recibo.getDenominacionObjeto()},
                    new String[]{"Carácter de recepción", caracterRecepcionLegible(relacion == null ? null : relacion.getTipoDeposito())}
            ));
            if (relacion != null && relacion.getFechaVencimiento() != null) {
                datosObjeto.add(new String[]{"Fecha de vencimiento", relacion.getFechaVencimiento().format(FORMATO_FECHA_VENCIMIENTO)});
            }
            datosObjeto.add(new String[]{"Descripción", recibo.getDescripcionBreve()});
            agregarDatos(documento, datosObjeto.toArray(String[][]::new));
            agregarConstancia(documento, recibo.getTextoConstancia());
            agregarFirmas(documento, recibo.getOperador());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el comprobante de recepción", ex);
        }
        return salida.toByteArray();
    }

    private void agregarEncabezado(Document documento) throws IOException {
        ClassPathResource logoResource = new ClassPathResource("pdf/logo-mmaas.png");
        try (InputStream logoStream = logoResource.getInputStream()) {
            Image logo = new Image(ImageDataFactory.create(logoStream.readAllBytes()));
            logo.scaleToFit(100, 100);
            logo.setHorizontalAlignment(HorizontalAlignment.CENTER);
            documento.add(logo);
        }
        documento.add(new Paragraph(INSTITUCION)
                .setFontSize(13).setBold().setFontColor(PRIMARIO)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(3).setMarginBottom(2));
        documento.add(new Paragraph("COMPROBANTE DE RECEPCIÓN DE OBJETO")
                .setFontSize(15).setBold().setFontColor(PRIMARIO)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(13).setMarginBottom(2));
        documento.add(new Paragraph("Recepción provisoria")
                .setFontSize(10).setItalic().setFontColor(SECUNDARIO).setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(0).setMarginBottom(15));
        SolidLine linea = new SolidLine(1.2f);
        linea.setColor(ACENTO);
        documento.add(new LineSeparator(linea).setMarginTop(0).setMarginBottom(0));
    }

    private void agregarIdentificacion(Document documento, ReciboIngresoObjeto recibo) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{58, 42})).useAllAvailableWidth();
        tabla.setMarginTop(12).setMarginBottom(14);
        tabla.addCell(celdaValor("N.º " + texto(recibo.getNumeroRecibo()), true, 13));
        tabla.addCell(celdaValor("Fecha: " + recibo.getFechaEmision().format(FORMATO_FECHA), true, 10)
                .setTextAlignment(TextAlignment.RIGHT));
        documento.add(tabla);
    }

    private void agregarSeccion(Document documento, String titulo) {
        documento.add(new Paragraph(titulo).setFontSize(9).setBold().setFontColor(PRIMARIO)
                .setMarginTop(5).setMarginBottom(5));
    }

    private void agregarDatos(Document documento, String[][] filas) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{28, 72})).useAllAvailableWidth();
        tabla.setMarginBottom(12);
        for (String[] fila : filas) {
            tabla.addCell(new Cell().add(new Paragraph(fila[0]).setFontSize(9).setBold().setFontColor(SECUNDARIO))
                    .setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(SECUNDARIO, 0.55f))
                    .setPaddingTop(7).setPaddingBottom(7).setPaddingLeft(8));
            tabla.addCell(new Cell().add(new Paragraph(texto(fila[1])).setFontSize(10))
                    .setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(SECUNDARIO, 0.55f))
                    .setPaddingTop(7).setPaddingBottom(7).setPaddingLeft(9).setPaddingRight(8));
        }
        documento.add(tabla);
    }

    private void agregarConstancia(Document documento, String constancia) {
        documento.add(new Paragraph("CONSTANCIA").setFontSize(9).setBold().setFontColor(SECUNDARIO)
                .setMarginTop(3).setMarginBottom(5));
        documento.add(new Paragraph(texto(constancia)).setFontSize(10).setFixedLeading(15)
                .setPadding(11).setBorderLeft(new SolidBorder(ACENTO, 2.2f)).setMarginBottom(22));
    }

    private void agregarFirmas(Document documento, String operador) {
        Table firmas = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
        firmas.setMarginTop(12);
        firmas.addCell(celdaFirma("Firma del depositante", ""));
        firmas.addCell(celdaFirma("Firma del responsable del museo", texto(operador)));
        documento.add(firmas);
    }

    private Cell celdaFirma(String etiqueta, String responsable) {
        Cell celda = new Cell().setBorder(Border.NO_BORDER).setPaddingLeft(12).setPaddingRight(12).setPaddingTop(35);
        SolidLine lineaFirma = new SolidLine(0.7f);
        lineaFirma.setColor(SECUNDARIO);
        celda.add(new LineSeparator(lineaFirma).setMarginBottom(6));
        if (!responsable.isBlank() && !responsable.equals("-")) {
            celda.add(new Paragraph(responsable).setFontSize(9).setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
        }
        celda.add(new Paragraph(etiqueta).setFontSize(9).setFontColor(SECUNDARIO)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(0));
        return celda;
    }

    private Cell celdaValor(String valor, boolean bold, float fontSize) {
        Paragraph parrafo = new Paragraph(valor).setFontSize(fontSize);
        if (bold) {
            parrafo.setBold();
        }
        return new Cell().add(parrafo).setBorder(Border.NO_BORDER).setPadding(0);
    }

    private String texto(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private void agregarDatoSiExiste(List<String[]> datos, String etiqueta, String valor) {
        if (valor != null && !valor.isBlank()) {
            datos.add(new String[]{etiqueta, valor.trim()});
        }
    }

    private String telefonoDesdeObservaciones(String observaciones) {
        if (observaciones == null || observaciones.isBlank()) {
            return null;
        }
        String prefijo = "Telefono: ";
        return observaciones.lines()
                .map(String::trim)
                .filter(linea -> linea.startsWith(prefijo))
                .map(linea -> linea.substring(prefijo.length()).trim())
                .filter(valor -> !valor.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String caracterRecepcionLegible(CaracterRecepcionObjeto caracter) {
        if (caracter == null) {
            return "-";
        }
        return switch (caracter) {
            case PRESTAMO -> "Préstamo";
            case COMODATO -> "Comodato";
            case DONACION -> "Donación";
            case COMPRA -> "Compra";
            case ESTUDIO -> "Estudio";
            case OTRO -> "Otro";
            case RECEPCION -> "Recepción";
        };
    }
}
