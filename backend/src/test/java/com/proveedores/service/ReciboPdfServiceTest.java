package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.ObjetoDepositante;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.ReciboIngresoObjeto;
import com.proveedores.repository.ObjetoDepositanteRepository;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReciboPdfServiceTest {

    @Mock
    private ObjetoDepositanteRepository objetoDepositanteRepository;

    @InjectMocks
    private ReciboPdfService reciboPdfService;

    @Test
    void imprimeDonacionConEtiquetaLegibleYSinFilaDeVencimiento() throws Exception {
        String texto = generarTexto(CaracterRecepcionObjeto.DONACION, null);

        assertThat(texto).contains("Carácter de recepción", "Donación")
                .doesNotContain("Fecha de vencimiento");
    }

    @Test
    void imprimePrestamoConFechaDeVencimientoDebajoDelCaracter() throws Exception {
        String texto = generarTexto(CaracterRecepcionObjeto.PRESTAMO, LocalDate.of(2026, 11, 4));

        assertThat(texto).contains("Carácter de recepción", "Préstamo", "Fecha de vencimiento", "04/11/2026");
        assertThat(texto.indexOf("Préstamo")).isLessThan(texto.indexOf("Fecha de vencimiento"));
    }

    private String generarTexto(CaracterRecepcionObjeto caracter, LocalDate fechaVencimiento) throws Exception {
        ObjetoMuseo objeto = new ObjetoMuseo();
        objeto.setId(42L);
        ObjetoDepositante relacion = new ObjetoDepositante();
        relacion.setTipoDeposito(caracter);
        relacion.setFechaVencimiento(fechaVencimiento);
        when(objetoDepositanteRepository.findRelacionActivaPorObjeto(42L)).thenReturn(Optional.of(relacion));

        ReciboIngresoObjeto recibo = new ReciboIngresoObjeto();
        recibo.setNumeroRecibo("REC-TEST-42");
        recibo.setFechaEmision(LocalDateTime.of(2026, 10, 5, 14, 30));
        recibo.setObjetoMuseo(objeto);
        recibo.setNumeroInventario("INV-TEST-42");
        recibo.setDenominacionObjeto("Objeto de prueba");
        recibo.setDescripcionBreve("Descripción breve de prueba");
        recibo.setDepositanteNombre("Depositante de prueba");
        recibo.setTextoConstancia("Constancia de prueba");

        byte[] pdf = reciboPdfService.generar(recibo);
        try (PdfDocument documento = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            assertThat(documento.getNumberOfPages()).isEqualTo(1);
            return PdfTextExtractor.getTextFromPage(documento.getPage(1));
        }
    }
}
