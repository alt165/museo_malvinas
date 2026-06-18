package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.dto.VeteranoRequestDTO;
import com.proveedores.dto.VeteranoVideoRequestDTO;
import com.proveedores.entity.Fuerza;
import com.proveedores.exception.BusinessException;
import com.proveedores.repository.VeteranoImagenRepository;
import com.proveedores.repository.VeteranoVideoRepository;
import com.proveedores.service.VeteranoImagenService;
import com.proveedores.service.VeteranoService;
import com.proveedores.service.VeteranoVideoService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

class VeteranoMultimediaIntegrationTest extends IntegrationTestBase {

    @Autowired
    private VeteranoService veteranoService;

    @Autowired
    private VeteranoImagenService veteranoImagenService;

    @Autowired
    private VeteranoVideoService veteranoVideoService;

    @Autowired
    private VeteranoImagenRepository veteranoImagenRepository;

    @Autowired
    private VeteranoVideoRepository veteranoVideoRepository;

    @Test
    void subirImagenValidaListaDescargaYElimina() {
        Long veteranoId = crearVeterano("Imagen");
        MockMultipartFile imagen = new MockMultipartFile("archivo", "veterano.webp", "image/webp", "imagen".getBytes());

        var response = veteranoImagenService.subir(veteranoId, imagen, "Retrato", "tester");

        assertThat(response.id()).isNotNull();
        assertThat(response.nombreArchivo()).isEqualTo("veterano.webp");
        assertThat(response.veteranoId()).isEqualTo(veteranoId);
        assertThat(veteranoImagenService.listar(veteranoId)).extracting("id").contains(response.id());
        assertThat(veteranoImagenService.descargar(veteranoId, response.id()).resource().exists()).isTrue();

        veteranoImagenService.eliminar(veteranoId, response.id());

        assertThat(veteranoImagenRepository.findById(response.id())).get()
                .satisfies(entity -> assertThat(entity.getEliminado()).isTrue());
        assertThat(veteranoImagenService.listar(veteranoId)).extracting("id").doesNotContain(response.id());
    }

    @Test
    void rechazaImagenConTipoInvalido() {
        Long veteranoId = crearVeterano("TipoInvalido");
        MockMultipartFile archivo = new MockMultipartFile("archivo", "foto.txt", "text/plain", "texto".getBytes());

        assertThatThrownBy(() -> veteranoImagenService.subir(veteranoId, archivo, null, "tester"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tipo de imagen no permitido");
    }

    @Test
    void creaVideoConUrlValidaYExtraeVideoId() {
        Long veteranoId = crearVeterano("Video");

        var response = veteranoVideoService.crear(veteranoId, new VeteranoVideoRequestDTO(
                "Entrevista",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "Testimonio completo",
                LocalDate.of(2024, 4, 2),
                null
        ));

        assertThat(response.id()).isNotNull();
        assertThat(response.videoId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(veteranoVideoService.listar(veteranoId)).extracting("id").contains(response.id());
    }

    @Test
    void aceptaFormatosYoutubeCortosYEmbed() {
        Long veteranoId = crearVeterano("Formatos");

        var corto = veteranoVideoService.crear(veteranoId, new VeteranoVideoRequestDTO("Corto", "https://youtu.be/abcDEF12345", null, null, null));
        var embed = veteranoVideoService.crear(veteranoId, new VeteranoVideoRequestDTO("Embed", "https://www.youtube.com/embed/ZYX98765432", null, null, null));

        assertThat(corto.videoId()).isEqualTo("abcDEF12345");
        assertThat(embed.videoId()).isEqualTo("ZYX98765432");
    }

    @Test
    void rechazaUrlQueNoEsYoutube() {
        Long veteranoId = crearVeterano("NoYoutube");

        assertThatThrownBy(() -> veteranoVideoService.crear(veteranoId, new VeteranoVideoRequestDTO(
                "No valido",
                "https://example.com/watch?v=dQw4w9WgXcQ",
                null,
                null,
                null
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("La URL debe ser de YouTube");
    }

    @Test
    void actualizaYDaDeBajaVideo() {
        Long veteranoId = crearVeterano("EditarVideo");
        var creado = veteranoVideoService.crear(veteranoId, new VeteranoVideoRequestDTO("Inicial", "https://youtu.be/abcDEF12345", null, null, null));

        var actualizado = veteranoVideoService.actualizar(veteranoId, creado.id(), new VeteranoVideoRequestDTO(
                "Actualizado",
                "https://www.youtube.com/embed/ZYX98765432",
                "Nueva descripcion",
                null,
                3
        ));

        assertThat(actualizado.titulo()).isEqualTo("Actualizado");
        assertThat(actualizado.videoId()).isEqualTo("ZYX98765432");
        assertThat(actualizado.orden()).isEqualTo(3);

        veteranoVideoService.eliminar(veteranoId, creado.id());

        assertThat(veteranoVideoRepository.findById(creado.id())).get()
                .satisfies(entity -> assertThat(entity.getEliminado()).isTrue());
        assertThat(veteranoVideoService.listar(veteranoId)).extracting("id").doesNotContain(creado.id());
    }

    private Long crearVeterano(String sufijo) {
        return veteranoService.crear(new VeteranoRequestDTO(
                "Juan" + sufijo,
                "Veterano" + sufijo,
                Fuerza.EJERCITO,
                LocalDate.of(1945, 1, 1),
                null,
                "Historia"
        )).id();
    }
}
