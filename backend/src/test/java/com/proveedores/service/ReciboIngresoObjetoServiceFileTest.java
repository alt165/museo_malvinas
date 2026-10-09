package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.proveedores.entity.Depositante;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.ReciboIngresoObjeto;
import com.proveedores.exception.BusinessException;
import com.proveedores.repository.ReciboIngresoObjetoRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class ReciboIngresoObjetoServiceFileTest {

    @TempDir
    Path tempDir;

    @Mock
    private ReciboIngresoObjetoRepository repository;

    @Mock
    private ReciboPdfService pdfService;

    @Mock
    private AuditoriaObjetoService auditoriaService;

    @Test
    void copiaFirmadaUsaNombreFisicoGeneradoYContenidoValidado() throws Exception {
        ReciboIngresoObjeto recibo = recibo();
        when(repository.findById(1L)).thenReturn(Optional.of(recibo));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ReciboIngresoObjetoService service = service();
        byte[] pdf = validPdf();

        service.subirCopiaFirmada(
                1L,
                new MockMultipartFile("archivo", "../../firmado.pdf", "application/pdf", pdf),
                "tester"
        );

        Path stored = Path.of(recibo.getCopiaFirmadaRutaAlmacenamiento());
        assertThat(stored).isRegularFile();
        assertThat(stored.getFileName().toString()).matches("[0-9a-f-]{36}\\.pdf");
        assertThat(stored.getFileName().toString()).doesNotContain("firmado", "..");
        assertThat(Files.readAllBytes(stored)).containsExactly(pdf);
    }

    @Test
    void reemplazoInvalidoConservaArchivoYMetadataAnterior() throws Exception {
        ReciboIngresoObjeto recibo = recibo();
        Path anterior = tempDir.resolve("anterior.pdf");
        Files.write(anterior, validPdf());
        recibo.setCopiaFirmadaRutaAlmacenamiento(anterior.toString());
        recibo.setCopiaFirmadaNombreArchivo("anterior.pdf");
        when(repository.findById(1L)).thenReturn(Optional.of(recibo));
        ReciboIngresoObjetoService service = service();

        assertThatThrownBy(() -> service.subirCopiaFirmada(
                1L,
                new MockMultipartFile("archivo", "nuevo.pdf", "application/pdf", "contenido-falso".getBytes()),
                "tester"
        )).isInstanceOf(BusinessException.class);

        assertThat(anterior).isRegularFile();
        assertThat(recibo.getCopiaFirmadaRutaAlmacenamiento()).isEqualTo(anterior.toString());
        assertThat(recibo.getCopiaFirmadaNombreArchivo()).isEqualTo("anterior.pdf");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void reemplazoExitosoEliminaArchivoAnteriorSoloDespuesDelCommit() throws Exception {
        ReciboIngresoObjeto recibo = recibo();
        Path anterior = tempDir.resolve("anterior.pdf");
        Files.write(anterior, validPdf());
        recibo.setCopiaFirmadaRutaAlmacenamiento(anterior.toString());
        recibo.setCopiaFirmadaNombreArchivo("anterior.pdf");
        when(repository.findById(1L)).thenReturn(Optional.of(recibo));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ReciboIngresoObjetoService service = service();
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.subirCopiaFirmada(
                    1L,
                    new MockMultipartFile("archivo", "nuevo.pdf", "application/pdf", validPdf()),
                    "tester"
            );

            Path nuevo = Path.of(recibo.getCopiaFirmadaRutaAlmacenamiento());
            assertThat(anterior).isRegularFile();
            assertThat(nuevo).isRegularFile();

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
                synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            }

            assertThat(anterior).doesNotExist();
            assertThat(nuevo).isRegularFile();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rollbackDeBaseEliminaArchivoNuevo() throws Exception {
        ReciboIngresoObjeto recibo = recibo();
        when(repository.findById(1L)).thenReturn(Optional.of(recibo));
        when(repository.saveAndFlush(any())).thenThrow(new BusinessException("fallo db"));
        ReciboIngresoObjetoService service = service();
        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThatThrownBy(() -> service.subirCopiaFirmada(
                    1L,
                    new MockMultipartFile("archivo", "firmado.pdf", "application/pdf", validPdf()),
                    "tester"
            )).isInstanceOf(BusinessException.class).hasMessage("fallo db");

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        try (var files = Files.walk(tempDir)) {
            assertThat(files.filter(Files::isRegularFile)).isEmpty();
        }
    }

    private ReciboIngresoObjetoService service() {
        return new ReciboIngresoObjetoService(
                repository,
                pdfService,
                new UploadFileValidator(100_000_000L),
                new TransactionalFileLifecycle(),
                auditoriaService,
                tempDir.toString(),
                10
        );
    }

    private ReciboIngresoObjeto recibo() {
        ObjetoMuseo objeto = new ObjetoMuseo();
        objeto.setId(10L);
        Depositante depositante = new Depositante();
        depositante.setId(20L);
        ReciboIngresoObjeto recibo = new ReciboIngresoObjeto();
        recibo.setId(1L);
        recibo.setObjetoMuseo(objeto);
        recibo.setDepositante(depositante);
        recibo.setEliminado(false);
        return recibo;
    }

    private byte[] validPdf() {
        return "%PDF-1.4\n1 0 obj\nendobj\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    }
}
