package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.EstadoExhibicionObjeto;
import com.proveedores.entity.Exhibicion;
import com.proveedores.entity.ExhibicionObjeto;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.exception.BusinessException;
import com.proveedores.repository.ExhibicionObjetoRepository;
import com.proveedores.repository.ExhibicionRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExhibicionServiceTest {

    @Mock
    private ExhibicionRepository exhibicionRepository;
    @Mock
    private ExhibicionObjetoRepository exhibicionObjetoRepository;
    @Mock
    private ObjetoMuseoRepository objetoMuseoRepository;
    @Mock
    private AuditoriaObjetoService auditoriaObjetoService;

    @InjectMocks
    private ExhibicionService service;

    @Test
    void finalizarConObjetosPendientesLanzaBusinessException() {
        Exhibicion exhibicion = exhibicion();
        ExhibicionObjeto pendiente = new ExhibicionObjeto();
        pendiente.setEstado(EstadoExhibicionObjeto.PENDIENTE_REVISION);
        pendiente.setDevolucionVerificada(false);
        when(exhibicionRepository.findById(1L)).thenReturn(Optional.of(exhibicion));
        when(exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(1L)).thenReturn(List.of(pendiente));

        assertThatThrownBy(() -> service.finalizar(1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void finalizarSinPendientesCambiaEstadoAFinalizada() {
        Exhibicion exhibicion = exhibicion();
        ExhibicionObjeto devuelto = new ExhibicionObjeto();
        devuelto.setExhibicion(exhibicion);
        devuelto.setObjetoMuseo(objeto());
        devuelto.setEstado(EstadoExhibicionObjeto.DEVUELTO);
        devuelto.setDevolucionVerificada(true);
        when(exhibicionRepository.findById(1L)).thenReturn(Optional.of(exhibicion));
        when(exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(1L)).thenReturn(List.of(devuelto));
        when(exhibicionRepository.save(exhibicion)).thenReturn(exhibicion);

        assertThat(service.finalizar(1L).estado()).isEqualTo(EstadoExhibicion.FINALIZADA);
    }

    @Test
    void buscarDisponibilidadMarcaDisponibleSiNoHaySuperposicion() {
        ObjetoMuseo objeto = objeto();
        when(objetoMuseoRepository.buscarParaDisponibilidadExhibicion("INV", PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("numeroInventario"))))
                .thenReturn(new PageImpl<>(List.of(objeto)));
        when(exhibicionObjetoRepository.findByObjetoMuseoIdAndEliminadoFalse(10L)).thenReturn(List.of());

        var page = service.buscarObjetosDisponibilidad("INV", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null, PageRequest.of(0, 10));

        assertThat(page.getContent()).singleElement().satisfies(item -> assertThat(item.disponible()).isTrue());
    }

    @Test
    void buscarDisponibilidadMarcaNoDisponibleSiHayExhibicionPermanenteSuperpuesta() {
        ObjetoMuseo objeto = objeto();
        Exhibicion permanente = exhibicion();
        permanente.setId(2L);
        permanente.setNombre("Permanente");
        permanente.setFechaInicio(LocalDate.of(2026, 1, 1));
        permanente.setFechaFin(null);
        ExhibicionObjeto relacion = new ExhibicionObjeto();
        relacion.setExhibicion(permanente);
        relacion.setObjetoMuseo(objeto);
        relacion.setEliminado(false);
        when(objetoMuseoRepository.buscarParaDisponibilidadExhibicion("INV", PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("numeroInventario"))))
                .thenReturn(new PageImpl<>(List.of(objeto)));
        when(exhibicionObjetoRepository.findByObjetoMuseoIdAndEliminadoFalse(10L)).thenReturn(List.of(relacion));

        var page = service.buscarObjetosDisponibilidad("INV", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), null, PageRequest.of(0, 10));

        assertThat(page.getContent()).singleElement().satisfies(item -> {
            assertThat(item.disponible()).isFalse();
            assertThat(item.exhibicionConflictoNombre()).isEqualTo("Permanente");
            assertThat(item.exhibicionConflictoPermanente()).isTrue();
        });
    }

    @Test
    void buscarDisponibilidadEnEdicionIgnoraExhibicionActual() {
        ObjetoMuseo objeto = objeto();
        Exhibicion actual = exhibicion();
        actual.setFechaInicio(LocalDate.of(2026, 1, 1));
        actual.setFechaFin(LocalDate.of(2026, 12, 31));
        ExhibicionObjeto relacion = new ExhibicionObjeto();
        relacion.setExhibicion(actual);
        relacion.setObjetoMuseo(objeto);
        relacion.setEliminado(false);
        when(objetoMuseoRepository.buscarParaDisponibilidadExhibicion("INV", PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("numeroInventario"))))
                .thenReturn(new PageImpl<>(List.of(objeto)));
        when(exhibicionObjetoRepository.findByObjetoMuseoIdAndEliminadoFalse(10L)).thenReturn(List.of(relacion));

        var page = service.buscarObjetosDisponibilidad("INV", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10), 1L, PageRequest.of(0, 10));

        assertThat(page.getContent()).singleElement().satisfies(item -> assertThat(item.disponible()).isTrue());
    }


    @Test
    void buscarFinalizadasDevuelveSoloResultadosDelRepositorio() {
        Exhibicion finalizada = exhibicion();
        finalizada.setEstado(EstadoExhibicion.FINALIZADA);
        when(exhibicionRepository.buscarFinalizadasPorTexto("muestra", PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("nombre"))))
                .thenReturn(new PageImpl<>(List.of(finalizada)));
        when(exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(1L)).thenReturn(List.of());

        var page = service.buscarFinalizadas("muestra", PageRequest.of(0, 10));

        assertThat(page.getContent()).singleElement().satisfies(item -> assertThat(item.estado()).isEqualTo(EstadoExhibicion.FINALIZADA));
    }


    @Test
    void buscarFinalizadasSinTextoUsaConsultaSinFiltro() {
        Exhibicion finalizada = exhibicion();
        finalizada.setEstado(EstadoExhibicion.FINALIZADA);
        when(exhibicionRepository.findByEstadoAndEliminadoFalse(EstadoExhibicion.FINALIZADA, PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("nombre"))))
                .thenReturn(new PageImpl<>(List.of(finalizada)));
        when(exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(1L)).thenReturn(List.of());

        var page = service.buscarFinalizadas(null, PageRequest.of(0, 10));

        assertThat(page.getContent()).singleElement().satisfies(item -> assertThat(item.estado()).isEqualTo(EstadoExhibicion.FINALIZADA));
    }

    @Test
    void obtenerObjetosParaRepetirRechazaExhibicionNoFinalizada() {
        Exhibicion activa = exhibicion();
        activa.setEstado(EstadoExhibicion.ACTIVA);
        when(exhibicionRepository.findById(1L)).thenReturn(Optional.of(activa));

        assertThatThrownBy(() -> service.obtenerObjetosParaRepetir(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Solo se pueden repetir exhibiciones finalizadas");
    }

    @Test
    void obtenerObjetosParaRepetirMarcaDisponibleEnNuevoRangoSinConflictos() {
        Exhibicion original = exhibicion();
        original.setEstado(EstadoExhibicion.FINALIZADA);
        ObjetoMuseo objeto = objeto();
        ExhibicionObjeto relacion = new ExhibicionObjeto();
        relacion.setExhibicion(original);
        relacion.setObjetoMuseo(objeto);
        when(exhibicionRepository.findById(1L)).thenReturn(Optional.of(original));
        when(exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(1L)).thenReturn(List.of(relacion));
        when(exhibicionObjetoRepository.findByObjetoMuseoIdAndEliminadoFalse(10L)).thenReturn(List.of(relacion));

        var objetos = service.obtenerObjetosParaRepetir(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThat(objetos).singleElement().satisfies(item -> assertThat(item.disponible()).isTrue());
    }

    @Test
    void obtenerObjetosParaRepetirMarcaNoDisponibleConExhibicionPermanenteSuperpuesta() {
        Exhibicion original = exhibicion();
        original.setEstado(EstadoExhibicion.FINALIZADA);
        original.setFechaInicio(LocalDate.of(2025, 1, 1));
        original.setFechaFin(LocalDate.of(2025, 2, 1));
        ObjetoMuseo objeto = objeto();
        ExhibicionObjeto relacionOriginal = new ExhibicionObjeto();
        relacionOriginal.setExhibicion(original);
        relacionOriginal.setObjetoMuseo(objeto);
        Exhibicion permanente = exhibicion();
        permanente.setId(2L);
        permanente.setNombre("Muestra permanente");
        permanente.setFechaInicio(LocalDate.of(2026, 1, 1));
        permanente.setFechaFin(null);
        ExhibicionObjeto relacionConflicto = new ExhibicionObjeto();
        relacionConflicto.setExhibicion(permanente);
        relacionConflicto.setObjetoMuseo(objeto);
        when(exhibicionRepository.findById(1L)).thenReturn(Optional.of(original));
        when(exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(1L)).thenReturn(List.of(relacionOriginal));
        when(exhibicionObjetoRepository.findByObjetoMuseoIdAndEliminadoFalse(10L)).thenReturn(List.of(relacionOriginal, relacionConflicto));

        var objetos = service.obtenerObjetosParaRepetir(1L, LocalDate.of(2026, 3, 1), null);

        assertThat(objetos).singleElement().satisfies(item -> {
            assertThat(item.disponible()).isFalse();
            assertThat(item.exhibicionConflictoNombre()).isEqualTo("Muestra permanente");
            assertThat(item.exhibicionConflictoPermanente()).isTrue();
        });
    }

    private ObjetoMuseo objeto() {
        ObjetoMuseo objeto = new ObjetoMuseo();
        objeto.setId(10L);
        objeto.setNumeroInventario("INV-10");
        objeto.setDenominacionObjeto("Objeto");
        objeto.setEliminado(false);
        objeto.setActivo(true);
        return objeto;
    }

    private Exhibicion exhibicion() {
        Exhibicion exhibicion = new Exhibicion();
        exhibicion.setId(1L);
        exhibicion.setNombre("Muestra");
        exhibicion.setFechaInicio(LocalDate.now());
        exhibicion.setEstado(EstadoExhibicion.ACTIVA);
        exhibicion.setEliminado(false);
        return exhibicion;
    }
}
