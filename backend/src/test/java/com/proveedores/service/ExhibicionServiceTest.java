package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.EstadoExhibicionObjeto;
import com.proveedores.entity.Exhibicion;
import com.proveedores.entity.ExhibicionObjeto;
import com.proveedores.exception.BusinessException;
import com.proveedores.repository.ExhibicionObjetoRepository;
import com.proveedores.repository.ExhibicionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
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
        devuelto.setEstado(EstadoExhibicionObjeto.DEVUELTO);
        devuelto.setDevolucionVerificada(true);
        when(exhibicionRepository.findById(1L)).thenReturn(Optional.of(exhibicion));
        when(exhibicionObjetoRepository.findByExhibicionIdAndEliminadoFalse(1L)).thenReturn(List.of(devuelto));
        when(exhibicionRepository.save(exhibicion)).thenReturn(exhibicion);

        assertThat(service.finalizar(1L).estado()).isEqualTo(EstadoExhibicion.FINALIZADA);
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
