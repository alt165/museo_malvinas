package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.proveedores.dto.InventarioRequestDTO;
import com.proveedores.entity.EstadoConservacion;
import com.proveedores.entity.EstadoInventario;
import com.proveedores.entity.Inventario;
import com.proveedores.entity.MovimientoInventario;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.TipoMovimientoInventario;
import com.proveedores.entity.Ubicacion;
import com.proveedores.repository.InventarioRepository;
import com.proveedores.repository.MovimientoInventarioRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.repository.UbicacionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock
    private InventarioRepository inventarioRepository;
    @Mock
    private ObjetoMuseoRepository objetoMuseoRepository;
    @Mock
    private UbicacionRepository ubicacionRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @InjectMocks
    private InventarioService service;

    @Test
    void crearInventarioRegistraMovimientoDeIngreso() {
        ObjetoMuseo objeto = objeto(1L);
        Ubicacion ubicacion = ubicacion(2L, "Sala A");
        when(objetoMuseoRepository.findById(1L)).thenReturn(Optional.of(objeto));
        when(ubicacionRepository.findById(2L)).thenReturn(Optional.of(ubicacion));
        when(inventarioRepository.save(any(Inventario.class))).thenAnswer(invocation -> {
            Inventario entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        service.crear(request(1L, 2L, EstadoInventario.DISPONIBLE));

        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimientoInventario.INGRESO);
        assertThat(captor.getValue().getUbicacionDestino().getId()).isEqualTo(2L);
    }

    @Test
    void actualizarCambioUbicacionRegistraMovimiento() {
        ObjetoMuseo objeto = objeto(1L);
        Ubicacion origen = ubicacion(2L, "Sala A");
        Ubicacion destino = ubicacion(3L, "Sala B");
        Inventario inventario = inventario(10L, objeto, origen, EstadoInventario.DISPONIBLE);
        when(inventarioRepository.findById(10L)).thenReturn(Optional.of(inventario));
        when(objetoMuseoRepository.findById(1L)).thenReturn(Optional.of(objeto));
        when(ubicacionRepository.findById(3L)).thenReturn(Optional.of(destino));
        when(inventarioRepository.save(any(Inventario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.actualizar(10L, request(1L, 3L, EstadoInventario.DISPONIBLE));

        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimientoInventario.CAMBIO_UBICACION);
        assertThat(captor.getValue().getUbicacionOrigen().getId()).isEqualTo(2L);
        assertThat(captor.getValue().getUbicacionDestino().getId()).isEqualTo(3L);
    }

    private InventarioRequestDTO request(Long objetoId, Long ubicacionId, EstadoInventario estado) {
        return new InventarioRequestDTO(objetoId, ubicacionId, estado, EstadoConservacion.BUENO, LocalDate.now(), null, null);
    }

    private Inventario inventario(Long id, ObjetoMuseo objeto, Ubicacion ubicacion, EstadoInventario estado) {
        Inventario inventario = new Inventario();
        inventario.setId(id);
        inventario.setObjetoMuseo(objeto);
        inventario.setUbicacion(ubicacion);
        inventario.setEstado(estado);
        inventario.setEstadoConservacion(EstadoConservacion.BUENO);
        inventario.setFechaIngreso(LocalDate.now());
        inventario.setFechaUltimoMovimiento(LocalDateTime.now());
        inventario.setEliminado(false);
        return inventario;
    }

    private ObjetoMuseo objeto(Long id) {
        ObjetoMuseo objeto = new ObjetoMuseo();
        objeto.setId(id);
        objeto.setDenominacionObjeto("Objeto");
        objeto.setNumeroInventario("INV-" + id);
        objeto.setEliminado(false);
        return objeto;
    }

    private Ubicacion ubicacion(Long id, String nombre) {
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setId(id);
        ubicacion.setNombre(nombre);
        ubicacion.setEliminado(false);
        return ubicacion;
    }
}
