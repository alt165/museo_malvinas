package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.proveedores.entity.FotoObjetoMuseo;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.VisibilidadCampo;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.FotoObjetoMuseoRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class FotoObjetoMuseoServiceTest {

    @Mock private FotoObjetoMuseoRepository repository;
    @Mock private ObjetoMuseoService objetoMuseoService;
    @Mock private ObjectFileStorageService storage;
    @Mock private ImageWatermarkService watermarkService;
    private FotoObjetoMuseoService service;

    @BeforeEach
    void setUp() {
        service = new FotoObjetoMuseoService(repository, objetoMuseoService, storage, watermarkService, 5);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cargaCreaOriginalYPublicaSinExponerNombreInterno() {
        ObjetoMuseo objeto = objeto();
        byte[] original = {1, 2, 3};
        byte[] publica = {4, 5, 6};
        when(objetoMuseoService.buscarObjetoActivo(7L)).thenReturn(objeto);
        when(watermarkService.generar(original, "image/png"))
                .thenReturn(new ImageWatermarkService.GeneratedPublicImage(publica, "image/png", "png", 10, 10));
        when(storage.storeBytes(7L, "fotos/original", original, "INV-7.png", "png"))
                .thenReturn(stored("original/a.png", "a.png"));
        when(storage.storeBytes(7L, "fotos/public", publica, "INV-7.png", "png"))
                .thenReturn(stored("public/b.png", "b.png"));
        when(repository.save(any())).thenAnswer(invocation -> {
            FotoObjetoMuseo foto = invocation.getArgument(0);
            foto.setId(9L);
            return foto;
        });

        var response = service.subir(7L, new MockMultipartFile("archivo", "../../foto.png", "image/png", original), null, "tester");

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.nombreArchivoAlmacenado()).isNull();
        verify(storage).storeBytes(7L, "fotos/original", original, "INV-7.png", "png");
        verify(storage).storeBytes(7L, "fotos/public", publica, "INV-7.png", "png");
    }

    @Test
    void falloAlGuardarPublicaLimpiaOriginalYNoCreaRegistro() {
        byte[] original = {1};
        when(objetoMuseoService.buscarObjetoActivo(7L)).thenReturn(objeto());
        when(watermarkService.generar(original, "image/png"))
                .thenReturn(new ImageWatermarkService.GeneratedPublicImage(new byte[]{2}, "image/png", "png", 1, 1));
        when(storage.storeBytes(7L, "fotos/original", original, "INV-7.png", "png"))
                .thenReturn(stored("original/a.png", "a.png"));
        when(storage.storeBytes(7L, "fotos/public", new byte[]{2}, "INV-7.png", "png"))
                .thenThrow(new BusinessException("fallo"));

        assertThatThrownBy(() -> service.subir(7L, new MockMultipartFile("archivo", "foto.png", "image/png", original), null, "tester"))
                .isInstanceOf(BusinessException.class);
        verify(storage).delete("original/a.png");
        verify(repository, never()).save(any());
    }

    @Test
    void viewerRecibePublicaYNoPuedePedirOriginal() {
        FotoObjetoMuseo foto = foto(VisibilidadCampo.PUBLICO);
        when(objetoMuseoService.buscarObjetoActivo(7L)).thenReturn(objeto());
        when(repository.findByIdAndObjetoMuseoIdAndEliminadoFalse(9L, 7L)).thenReturn(Optional.of(foto));
        when(storage.load("public/b.png")).thenReturn(new ByteArrayResource(new byte[]{2}));
        authenticate("ROLE_VIEWER");

        var archivo = service.descargar(7L, 9L);

        assertThat(archivo.original()).isFalse();
        assertThat(archivo.contentType()).isEqualTo("image/png");
        verify(storage).load("public/b.png");
        assertThatThrownBy(() -> service.descargarOriginal(7L, 9L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void soloAdminDescargaElOriginalExacto() throws Exception {
        FotoObjetoMuseo foto = foto(VisibilidadCampo.PUBLICO);
        when(objetoMuseoService.buscarObjetoActivo(7L)).thenReturn(objeto());
        when(repository.findByIdAndObjetoMuseoIdAndEliminadoFalse(9L, 7L)).thenReturn(Optional.of(foto));
        when(storage.load("original/a.png")).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));
        authenticate("ROLE_ADMIN");

        var archivo = service.descargarOriginal(7L, 9L);

        assertThat(archivo.original()).isTrue();
        assertThat(archivo.resource().getInputStream().readAllBytes()).containsExactly(1, 2, 3);
        verify(storage).load("original/a.png");
        verify(storage, never()).load("public/b.png");
    }

    @Test
    void museologoNoPuedeDescargarOriginal() {
        authenticate("ROLE_MUSEOLOGO");

        assertThatThrownBy(() -> service.descargarOriginal(7L, 9L)).isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).findByIdAndObjetoMuseoIdAndEliminadoFalse(anyLong(), anyLong());
    }

    @Test
    void museologoVisualizaFotoPrivadaConVersionPublicaDistintaDelOriginal() throws Exception {
        FotoObjetoMuseo foto = foto(VisibilidadCampo.PRIVADO);
        when(objetoMuseoService.buscarObjetoActivo(7L)).thenReturn(objeto());
        when(repository.findByIdAndObjetoMuseoIdAndEliminadoFalse(9L, 7L)).thenReturn(Optional.of(foto));
        byte[] original = {1, 2, 3};
        byte[] publica = {4, 5, 6};
        when(storage.load("public/b.png")).thenReturn(new ByteArrayResource(publica));
        authenticate("ROLE_MUSEOLOGO");

        var archivo = service.descargar(7L, 9L);

        assertThat(archivo.original()).isFalse();
        assertThat(archivo.resource().getInputStream().readAllBytes())
                .containsExactly(publica)
                .isNotEqualTo(original);
        verify(storage).load("public/b.png");
        verify(storage, never()).load("original/a.png");
        assertThatThrownBy(() -> service.descargarOriginal(7L, 9L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminUsaVersionPublicaEnEndpointComunYOriginalSoloEnEndpointExplicito() throws Exception {
        FotoObjetoMuseo foto = foto(VisibilidadCampo.PUBLICO);
        when(objetoMuseoService.buscarObjetoActivo(7L)).thenReturn(objeto());
        when(repository.findByIdAndObjetoMuseoIdAndEliminadoFalse(9L, 7L)).thenReturn(Optional.of(foto));
        when(storage.load("public/b.png")).thenReturn(new ByteArrayResource(new byte[]{4, 5, 6}));
        when(storage.load("original/a.png")).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));
        authenticate("ROLE_ADMIN");

        var visualizacion = service.descargar(7L, 9L);
        var original = service.descargarOriginal(7L, 9L);

        assertThat(visualizacion.original()).isFalse();
        assertThat(visualizacion.resource().getInputStream().readAllBytes()).containsExactly(4, 5, 6);
        assertThat(original.original()).isTrue();
        assertThat(original.resource().getInputStream().readAllBytes()).containsExactly(1, 2, 3);
    }

    @Test
    void noDescargaUnaFotoInexistenteONoPertenecienteAlObjeto() {
        when(objetoMuseoService.buscarObjetoActivo(7L)).thenReturn(objeto());
        when(repository.findByIdAndObjetoMuseoIdAndEliminadoFalse(99L, 7L)).thenReturn(Optional.empty());
        authenticate("ROLE_ADMIN");

        assertThatThrownBy(() -> service.descargarOriginal(7L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Foto del objeto no encontrada");
        verify(storage, never()).load(anyString());
    }

    @Test
    void viewerNoPuedeVerFotoPrivada() {
        when(objetoMuseoService.buscarObjetoActivo(7L)).thenReturn(objeto());
        when(repository.findByIdAndObjetoMuseoIdAndEliminadoFalse(9L, 7L))
                .thenReturn(Optional.of(foto(VisibilidadCampo.PRIVADO)));
        authenticate("ROLE_VIEWER");

        assertThatThrownBy(() -> service.descargar(7L, 9L)).isInstanceOf(ResourceNotFoundException.class);
        verify(storage, never()).load(anyString());
    }

    @Test
    void regeneracionEsIdempotente() {
        FotoObjetoMuseo existente = foto(VisibilidadCampo.PUBLICO);
        FotoObjetoMuseo faltante = foto(VisibilidadCampo.PUBLICO);
        faltante.setId(10L);
        faltante.setRutaPublica(null);
        when(repository.findByEliminadoFalseOrderByIdAsc()).thenReturn(List.of(existente, faltante), List.of(existente, faltante));
        when(storage.exists("public/b.png")).thenReturn(true);
        when(storage.load("original/a.png")).thenReturn(new ByteArrayResource(new byte[]{1}));
        when(watermarkService.generar(new byte[]{1}, "image/png"))
                .thenReturn(new ImageWatermarkService.GeneratedPublicImage(new byte[]{2}, "image/png", "png", 1, 1));
        when(storage.storeBytes(anyLong(), anyString(), any(), anyString(), anyString()))
                .thenReturn(stored("public/new.png", "new.png"));

        var first = service.regenerarPublicasFaltantes();
        when(storage.exists("public/new.png")).thenReturn(true);
        var second = service.regenerarPublicasFaltantes();

        assertThat(first.generadas()).isEqualTo(1);
        assertThat(first.omitidas()).isEqualTo(1);
        assertThat(second.generadas()).isZero();
        assertThat(second.omitidas()).isEqualTo(2);
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "tester", "n/a", Set.of(new SimpleGrantedAuthority(role))));
    }

    private ObjetoMuseo objeto() {
        ObjetoMuseo objeto = new ObjetoMuseo();
        objeto.setId(7L);
        objeto.setNumeroInventario("INV-7");
        return objeto;
    }

    private FotoObjetoMuseo foto(VisibilidadCampo visibilidad) {
        FotoObjetoMuseo foto = new FotoObjetoMuseo();
        foto.setId(9L);
        foto.setObjetoMuseo(objeto());
        foto.setNombreArchivo("INV-7.png");
        foto.setContentType("image/png");
        foto.setContentTypePublico("image/png");
        foto.setRutaRelativa("original/a.png");
        foto.setRutaPublica("public/b.png");
        foto.setVisibilidad(visibilidad);
        return foto;
    }

    private ObjectFileStorageService.StoredObjectFile stored(String path, String name) {
        return new ObjectFileStorageService.StoredObjectFile("INV-7.png", name, path, "/tmp/" + path);
    }
}
