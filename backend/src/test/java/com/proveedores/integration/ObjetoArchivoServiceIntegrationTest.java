package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.dto.CategoriaObjetoRequestDTO;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.EstadoConservacion;
import com.proveedores.exception.BusinessException;
import com.proveedores.repository.FotoObjetoMuseoRepository;
import com.proveedores.service.FotoObjetoMuseoService;
import com.proveedores.service.ObjetoMuseoService;
import com.proveedores.service.CategoriaObjetoService;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Set;
import javax.imageio.ImageIO;
import com.proveedores.service.ReciboEscaneadoObjetoMuseoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

class ObjetoArchivoServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjetoMuseoService objetoMuseoService;

    @Autowired
    private FotoObjetoMuseoService fotoObjetoMuseoService;

    @Autowired
    private ReciboEscaneadoObjetoMuseoService reciboEscaneadoObjetoMuseoService;

    @Autowired
    private FotoObjetoMuseoRepository fotoObjetoMuseoRepository;

    @Autowired
    private CategoriaObjetoService categoriaObjetoService;

    @Test
    void subirFotoValidaCreaAmbasVersionesListaDescargaYElimina() throws Exception {
        Long objetoId = crearObjeto("IT-FILE-FOTO-001");
        byte[] original = imagenPng(1800, 900);
        MockMultipartFile foto = new MockMultipartFile("archivo", "foto.png", "image/png", original);

        var response = fotoObjetoMuseoService.subir(objetoId, foto, "Vista frontal", "tester");

        assertThat(response.id()).isNotNull();
        assertThat(response.nombreArchivo()).matches("MMAS\\d{9}.png");
        assertThat(response.nombreArchivoAlmacenado()).isNull();
        assertThat(fotoObjetoMuseoService.listar(objetoId)).extracting("id").contains(response.id());
        var entity = fotoObjetoMuseoRepository.findById(response.id()).orElseThrow();
        assertThat(entity.getRutaRelativa()).contains("/original/");
        assertThat(entity.getRutaPublica()).contains("/public/");
        assertThat(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(entity.getRutaAlmacenamiento()))).containsExactly(original);
        BufferedImage publica = ImageIO.read(fotoObjetoMuseoService.descargar(objetoId, response.id()).resource().getInputStream());
        assertThat(publica.getWidth()).isEqualTo(1600);
        assertThat(publica.getHeight()).isEqualTo(800);

        fotoObjetoMuseoService.eliminar(objetoId, response.id());

        assertThat(fotoObjetoMuseoRepository.findById(response.id())).get()
                .satisfies(fotoEliminada -> assertThat(fotoEliminada.getEliminado()).isTrue());
        assertThat(fotoObjetoMuseoService.listar(objetoId)).extracting("id").doesNotContain(response.id());
    }

    @Test
    void rechazaFotoConTipoInvalido() {
        Long objetoId = crearObjeto("IT-FILE-FOTO-002");
        MockMultipartFile archivo = new MockMultipartFile("archivo", "foto.txt", "text/plain", "texto".getBytes());

        assertThatThrownBy(() -> fotoObjetoMuseoService.subir(objetoId, archivo, null, "tester"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tipo de imagen no permitido");
    }

    @Test
    void rechazaFotoMayorAlMaximo() {
        Long objetoId = crearObjeto("IT-FILE-FOTO-003");
        MockMultipartFile archivo = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[6 * 1024 * 1024]);

        assertThatThrownBy(() -> fotoObjetoMuseoService.subir(objetoId, archivo, null, "tester"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("La foto supera el tamano maximo permitido");
    }

    @Test
    void subirReciboEscaneadoOpcionalYReemplazaAnterior() {
        Long objetoId = crearObjeto("IT-FILE-REC-001");
        MockMultipartFile primero = new MockMultipartFile("archivo", "recibo.pdf", "application/pdf", "pdf".getBytes());
        MockMultipartFile segundo = new MockMultipartFile("archivo", "recibo.png", "image/png", "png".getBytes());

        var response = reciboEscaneadoObjetoMuseoService.subir(objetoId, primero, "tester");
        var reemplazo = reciboEscaneadoObjetoMuseoService.subir(objetoId, segundo, "tester");

        assertThat(response.id()).isNotEqualTo(reemplazo.id());
        assertThat(reciboEscaneadoObjetoMuseoService.obtener(objetoId)).get()
                .satisfies(item -> assertThat(item.id()).isEqualTo(reemplazo.id()));
        assertThat(reciboEscaneadoObjetoMuseoService.descargar(objetoId).resource().exists()).isTrue();

        reciboEscaneadoObjetoMuseoService.eliminar(objetoId, reemplazo.id());

        assertThat(reciboEscaneadoObjetoMuseoService.obtener(objetoId)).isEmpty();
    }

    private Long crearObjeto(String numeroInventario) {
        var categoria = categoriaObjetoService.crear(new CategoriaObjetoRequestDTO("Categoria " + numeroInventario, null));
        return objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                numeroInventario,
                "Objeto archivos " + numeroInventario,
                "Descripcion",
                "Descripcion tecnica", "Material", "10 cm", EstadoConservacion.BUENO, Set.of(categoria.id()),
                null,
                1L,
                CaracterRecepcionObjeto.DONACION,
                null
        )).id();
    }

    private byte[] imagenPng(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
