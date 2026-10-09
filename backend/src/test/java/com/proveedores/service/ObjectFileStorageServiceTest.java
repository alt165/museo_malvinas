package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.exception.BusinessException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ObjectFileStorageServiceTest {

    @TempDir
    java.nio.file.Path tempDir;

    @Test
    void almacenaContenidoExactoConNombreGenerado() throws Exception {
        ObjectFileStorageService service = new ObjectFileStorageService(tempDir.toString());
        byte[] original = {1, 2, 3, 4};

        var stored = service.storeBytes(12L, "fotos/original", original, "../../nombre.png", "png");

        assertThat(stored.relativePath()).startsWith("objeto-12/fotos/original/");
        assertThat(stored.storedName()).doesNotContain("nombre").doesNotContain("..");
        assertThat(Files.readAllBytes(tempDir.resolve(stored.relativePath()))).containsExactly(original);
    }

    @Test
    void bloqueaPathTraversalEnCarpetaYCarga() {
        ObjectFileStorageService service = new ObjectFileStorageService(tempDir.toString());

        assertThatThrownBy(() -> service.storeBytes(12L, "../../fuera", new byte[]{1}, "foto.png", "png"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Nombre de archivo invalido");
        assertThatThrownBy(() -> service.load("../../fuera.png"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ruta de archivo invalida");
    }

    @Test
    void nombreFisicoNuncaDerivaDelNombreOriginalMalicioso() {
        ObjectFileStorageService service = new ObjectFileStorageService(tempDir.toString());

        var stored = service.storeBytesInOwnerFolder(
                "veterano-4", "imagenes", new byte[]{1}, "..\\..\\malicioso.png\r\n", "png");

        assertThat(stored.storedName()).matches("[0-9a-f-]{36}\\.png");
        assertThat(stored.relativePath()).startsWith("veterano-4/imagenes/");
        assertThat(stored.relativePath()).doesNotContain("malicioso", "..", "\\");
        assertThat(tempDir.resolve(stored.relativePath())).isRegularFile();
    }
}
