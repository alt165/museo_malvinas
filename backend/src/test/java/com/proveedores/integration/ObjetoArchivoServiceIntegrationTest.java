package com.proveedores.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.exception.BusinessException;
import com.proveedores.repository.FotoObjetoMuseoRepository;
import com.proveedores.service.FotoObjetoMuseoService;
import com.proveedores.service.ObjetoMuseoService;
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

    @Test
    void subirFotoValidaListaDescargaYElimina() {
        Long objetoId = crearObjeto("IT-FILE-FOTO-001");
        MockMultipartFile foto = new MockMultipartFile("archivo", "foto.webp", "image/webp", "imagen".getBytes());

        var response = fotoObjetoMuseoService.subir(objetoId, foto, "Vista frontal", "tester");

        assertThat(response.id()).isNotNull();
        assertThat(response.nombreArchivo()).isEqualTo("foto.webp");
        assertThat(fotoObjetoMuseoService.listar(objetoId)).extracting("id").contains(response.id());
        assertThat(fotoObjetoMuseoService.descargar(objetoId, response.id()).resource().exists()).isTrue();

        fotoObjetoMuseoService.eliminar(objetoId, response.id());

        assertThat(fotoObjetoMuseoRepository.findById(response.id())).get()
                .satisfies(entity -> assertThat(entity.getEliminado()).isTrue());
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
        return objetoMuseoService.crear(new ObjetoMuseoRequestDTO(
                numeroInventario,
                "Objeto archivos " + numeroInventario,
                null,
                null, null, null, null, null,
                null,
                1L,
                CaracterRecepcionObjeto.DONACION,
                null
        )).id();
    }
}
