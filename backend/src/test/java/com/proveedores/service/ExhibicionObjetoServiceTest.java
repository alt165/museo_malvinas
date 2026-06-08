package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.proveedores.dto.ExhibicionObjetoRequestDTO;
import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.EstadoExhibicionObjeto;
import com.proveedores.entity.Exhibicion;
import com.proveedores.entity.ExhibicionObjeto;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.Usuario;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.ExhibicionObjetoRepository;
import com.proveedores.repository.ExhibicionRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExhibicionObjetoServiceTest {

    @Mock
    private ExhibicionObjetoRepository exhibicionObjetoRepository;
    @Mock
    private ExhibicionRepository exhibicionRepository;
    @Mock
    private ObjetoMuseoRepository objetoMuseoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditoriaObjetoService auditoriaObjetoService;

    @InjectMocks
    private ExhibicionObjetoService service;

    @Test
    void noPermiteObjetoEnMasDeUnaExhibicionActiva() {
        Exhibicion nueva = exhibicion(1L);
        Exhibicion activaExistente = exhibicion(2L);
        ExhibicionObjeto existente = new ExhibicionObjeto();
        existente.setId(50L);
        existente.setExhibicion(activaExistente);
        existente.setEliminado(false);
        when(exhibicionRepository.findById(1L)).thenReturn(Optional.of(nueva));
        when(objetoMuseoRepository.findById(10L)).thenReturn(Optional.of(objeto()));
        when(exhibicionObjetoRepository.findByObjetoMuseoIdAndEliminadoFalse(10L)).thenReturn(List.of(existente));

        assertThatThrownBy(() -> service.crear(request())).isInstanceOf(BusinessException.class);
    }

    @Test
    void verificarDevolucionActualizaEstadoYUsuario() {
        ExhibicionObjeto relacion = new ExhibicionObjeto();
        relacion.setId(1L);
        relacion.setExhibicion(exhibicion(1L));
        relacion.setObjetoMuseo(objeto());
        relacion.setEstado(EstadoExhibicionObjeto.EN_EXHIBICION);
        relacion.setDevolucionVerificada(false);
        relacion.setEliminado(false);
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNombre("Operador");
        usuario.setEliminado(false);
        when(exhibicionObjetoRepository.findById(1L)).thenReturn(Optional.of(relacion));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(exhibicionObjetoRepository.save(any(ExhibicionObjeto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.verificarDevolucion(1L, 7L, "OK");

        assertThat(response.estado()).isEqualTo(EstadoExhibicionObjeto.DEVUELTO);
        assertThat(response.devolucionVerificada()).isTrue();
        assertThat(response.verificadoPorUsuarioId()).isEqualTo(7L);
    }

    @Test
    void crearConObjetoInexistenteLanzaResourceNotFoundException() {
        when(exhibicionRepository.findById(1L)).thenReturn(Optional.of(exhibicion(1L)));
        when(objetoMuseoRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(request())).isInstanceOf(ResourceNotFoundException.class);
    }

    private ExhibicionObjetoRequestDTO request() {
        return new ExhibicionObjetoRequestDTO(1L, 10L, LocalDate.now(), null, EstadoExhibicionObjeto.EN_EXHIBICION, false, null, null, null);
    }

    private Exhibicion exhibicion(Long id) {
        Exhibicion exhibicion = new Exhibicion();
        exhibicion.setId(id);
        exhibicion.setNombre("Muestra " + id);
        exhibicion.setEstado(EstadoExhibicion.ACTIVA);
        exhibicion.setEliminado(false);
        return exhibicion;
    }

    private ObjetoMuseo objeto() {
        ObjetoMuseo objeto = new ObjetoMuseo();
        objeto.setId(10L);
        objeto.setDenominacionObjeto("Objeto");
        objeto.setNumeroInventario("INV-10");
        objeto.setEliminado(false);
        return objeto;
    }
}
