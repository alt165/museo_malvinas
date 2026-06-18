package com.proveedores.service;

import com.proveedores.entity.ReciboIngresoObjeto;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReciboPdfService {

    public byte[] generar(ReciboIngresoObjeto recibo) {
        List<String> lineas = new ArrayList<>();
        lineas.add("Recibo de ingreso " + recibo.getNumeroRecibo());
        lineas.add("Fecha: " + recibo.getFechaEmision().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        lineas.add("Depositante: " + recibo.getDepositanteNombre());
        lineas.add("Contacto: " + texto(recibo.getDepositanteContacto()));
        lineas.add("Numero de inventario: " + recibo.getNumeroInventario());
        lineas.add("Objeto: " + recibo.getDenominacionObjeto());
        lineas.addAll(partirLinea("Descripcion: " + recibo.getDescripcionBreve()));
        lineas.add("Operador: " + texto(recibo.getOperador()));
        lineas.add("");
        lineas.addAll(partirLinea(recibo.getTextoConstancia()));
        lineas.add("");
        lineas.add("Firma depositante: ______________________________");
        return crearPdfSimple(lineas);
    }

    private byte[] crearPdfSimple(List<String> lineas) {
        StringBuilder contenido = new StringBuilder("BT /F1 12 Tf ");
        int y = 780;
        for (String linea : lineas) {
            contenido.append("1 0 0 1 50 ").append(y).append(" Tm (").append(escape(linea)).append(") Tj ");
            y -= 18;
        }
        contenido.append("ET");

        String stream = contenido.toString();
        String pdf = "%PDF-1.4\n"
                + "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
                + "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n"
                + "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n"
                + "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n"
                + "5 0 obj << /Length " + stream.getBytes(StandardCharsets.ISO_8859_1).length + " >> stream\n"
                + stream + "\nendstream endobj\n"
                + "trailer << /Root 1 0 R >>\n%%EOF\n";
        return pdf.getBytes(StandardCharsets.ISO_8859_1);
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private String texto(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private List<String> partirLinea(String value) {
        int max = 78;
        List<String> resultado = new ArrayList<>();
        String restante = value == null ? "" : value.trim();
        while (restante.length() > max) {
            int corte = restante.lastIndexOf(' ', max);
            if (corte <= 0) {
                corte = max;
            }
            resultado.add(restante.substring(0, corte).trim());
            restante = restante.substring(corte).trim();
        }
        resultado.add(restante);
        return resultado;
    }
}
