package com.proveedores.report;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PdfReportService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DeviceRgb PRIMARY = new DeviceRgb(29, 78, 74);
    private static final DeviceRgb HEADER_BACKGROUND = new DeviceRgb(234, 240, 238);
    private static final DeviceRgb BORDER = new DeviceRgb(196, 207, 204);

    public <T> byte[] generate(TabularReport<T> report, ReportMetadata metadata) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(output);
            PdfDocument pdf = new PdfDocument(writer);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new HeaderFooterHandler(metadata, report.title(), regular, bold));

            Document document = new Document(pdf, PageSize.A4.rotate());
            document.setMargins(86, 24, 48, 24);
            document.setFont(regular);

            addReportSummary(document, report, metadata, bold);
            document.add(buildTable(report, bold));
            document.close();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el PDF", exception);
        }
    }

    private <T> void addReportSummary(Document document, TabularReport<T> report, ReportMetadata metadata, PdfFont bold) {
        document.add(new Paragraph("Fecha y hora de generación: " + DATE_TIME_FORMAT.format(metadata.generatedAt()))
                .setFontSize(9)
                .setMarginBottom(2));
        document.add(new Paragraph("Usuario: " + text(metadata.generatedBy()))
                .setFontSize(9)
                .setMarginBottom(8));
        document.add(new Paragraph(report.summaryTitle())
                .setFont(bold)
                .setFontSize(10)
                .setMarginBottom(3));

        List<ReportFilter> filters = report.filters();
        if (filters == null || filters.isEmpty()) {
            document.add(new Paragraph("Sin filtros")
                    .setFontSize(9)
                    .setMarginBottom(8));
        } else {
            for (ReportFilter filter : filters) {
                document.add(new Paragraph(filter.label() + ": " + text(filter.value()))
                        .setFontSize(9)
                        .setMarginBottom(1));
            }
            document.add(new Paragraph(" ").setFontSize(2).setMarginBottom(4));
        }

        document.add(new Paragraph("Total de registros exportados: " + report.rows().size())
                .setFontSize(9)
                .setMarginBottom(8));
    }

    private <T> Table buildTable(TabularReport<T> report, PdfFont bold) {
        float[] widths = report.columns().stream().mapToDouble(ReportColumn::width).collect(
                () -> new FloatArrayBuilder(report.columns().size()),
                FloatArrayBuilder::add,
                FloatArrayBuilder::addAll
        ).toArray();
        Table table = new Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth();
        table.setFontSize(7.4f);

        for (ReportColumn<T> column : report.columns()) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(column.header()).setFont(bold))
                    .setBackgroundColor(PRIMARY)
                    .setFontColor(ColorConstants.WHITE)
                    .setPadding(5)
                    .setBorder(new SolidBorder(PRIMARY, 0.5f))
                    .setTextAlignment(TextAlignment.LEFT));
        }

        if (report.rows().isEmpty()) {
            table.addCell(new Cell(1, report.columns().size())
                    .add(new Paragraph(text(report.emptyMessage())))
                    .setPadding(8)
                    .setBorder(new SolidBorder(BORDER, 0.5f)));
            return table;
        }

        int index = 0;
        for (T row : report.rows()) {
            boolean shaded = index % 2 == 1;
            for (ReportColumn<T> column : report.columns()) {
                Cell cell = new Cell()
                        .add(new Paragraph(text(column.valueExtractor().apply(row))).setMultipliedLeading(1.05f))
                        .setPadding(4)
                        .setBorder(new SolidBorder(BORDER, 0.4f))
                        .setVerticalAlignment(VerticalAlignment.TOP);
                if (shaded) {
                    cell.setBackgroundColor(HEADER_BACKGROUND);
                }
                table.addCell(cell);
            }
            index++;
        }

        return table;
    }

    private String text(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static class HeaderFooterHandler implements IEventHandler {
        private final ReportMetadata metadata;
        private final String title;
        private final PdfFont regular;
        private final PdfFont bold;

        HeaderFooterHandler(ReportMetadata metadata, String title, PdfFont regular, PdfFont bold) {
            this.metadata = metadata;
            this.title = title;
            this.regular = regular;
            this.bold = bold;
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent documentEvent = (PdfDocumentEvent) event;
            PdfDocument pdf = documentEvent.getDocument();
            PdfPage page = documentEvent.getPage();
            Rectangle pageSize = page.getPageSize();
            int pageNumber = pdf.getPageNumber(page);

            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdf);
            try (Canvas canvas = new Canvas(pdfCanvas, pageSize)) {
                canvas.showTextAligned(new Paragraph(metadata.institutionName()).setFont(bold).setFontSize(10).setFontColor(PRIMARY),
                        pageSize.getLeft() + 24, pageSize.getTop() - 24, TextAlignment.LEFT);
                canvas.showTextAligned(new Paragraph(title).setFont(bold).setFontSize(13),
                        pageSize.getLeft() + 24, pageSize.getTop() - 42, TextAlignment.LEFT);
                canvas.showTextAligned(new Paragraph("Generado: " + DATE_TIME_FORMAT.format(metadata.generatedAt())).setFont(regular).setFontSize(8),
                        pageSize.getRight() - 24, pageSize.getTop() - 24, TextAlignment.RIGHT);
                canvas.showTextAligned(new Paragraph("Página " + pageNumber).setFont(regular).setFontSize(8),
                        pageSize.getRight() - 24, pageSize.getBottom() + 22, TextAlignment.RIGHT);
            }
        }
    }

    private static class FloatArrayBuilder {
        private final float[] values;
        private int index;

        FloatArrayBuilder(int size) {
            this.values = new float[size];
        }

        void add(double value) {
            values[index++] = (float) value;
        }

        void addAll(FloatArrayBuilder other) {
            for (int i = 0; i < other.index; i++) {
                add(other.values[i]);
            }
        }

        float[] toArray() {
            return values;
        }
    }
}
