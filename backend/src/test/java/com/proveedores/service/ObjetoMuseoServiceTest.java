package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.Depositante;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.ReciboIngresoObjeto;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.CategoriaObjetoRepository;
import com.proveedores.repository.DepositanteRepository;
import com.proveedores.repository.FotoObjetoMuseoRepository;
import com.proveedores.repository.InventarioRepository;
import com.proveedores.repository.MovimientoInventarioRepository;
import com.proveedores.repository.ObjetoCategoriaRepository;
import com.proveedores.repository.ObjetoDepositanteRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.repository.ReciboEscaneadoObjetoMuseoRepository;
import com.proveedores.repository.ReciboIngresoObjetoRepository;
import com.proveedores.repository.UbicacionRepository;
import com.proveedores.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObjetoMuseoServiceTest {

    @Mock
    private ObjetoMuseoRepository objetoMuseoRepository;

    @Mock
    private CategoriaObjetoRepository categoriaObjetoRepository;

    @Mock
    private ObjetoCategoriaRepository objetoCategoriaRepository;

    @Mock
    private DepositanteRepository depositanteRepository;

    @Mock
    private ObjetoDepositanteRepository objetoDepositanteRepository;

    @Mock
    private ReciboIngresoObjetoRepository reciboIngresoObjetoRepository;

    @Mock
    private FotoObjetoMuseoRepository fotoObjetoMuseoRepository;

    @Mock
    private ReciboEscaneadoObjetoMuseoRepository reciboEscaneadoObjetoMuseoRepository;

    @Mock
    private InventarioRepository inventarioRepository;

    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Mock
    private UbicacionRepository ubicacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ObjetoMuseoService service;

    @Test
    void crearConNumeroInventarioDuplicadoLanzaBusinessException() {
        ObjetoMuseo existente = objeto(1L, "INV-1");
        when(objetoMuseoRepository.findByNumeroInventario("INV-1")).thenReturn(Optional.of(existente));

        ObjetoMuseoRequestDTO request = new ObjetoMuseoRequestDTO("INV-1", "Casco", null, null, null, null, null, null);

        assertThatThrownBy(() -> service.crear(request)).isInstanceOf(BusinessException.class);
        verify(objetoMuseoRepository, never()).save(any());
    }

    @Test
    void crearObjetoValidoDevuelveResponse() {
        when(objetoMuseoRepository.findByNumeroInventario("INV-2")).thenReturn(Optional.empty());
        Depositante depositante = new Depositante();
        depositante.setId(3L);
        depositante.setNombre("Depositante test");
        depositante.setEliminado(false);
        when(depositanteRepository.findById(3L)).thenReturn(Optional.of(depositante));
        when(objetoMuseoRepository.save(any(ObjetoMuseo.class))).thenAnswer(invocation -> {
            ObjetoMuseo entity = invocation.getArgument(0);
            entity.setId(2L);
            return entity;
        });
        when(objetoCategoriaRepository.findByObjetoMuseoIdAndEliminadoFalse(2L)).thenReturn(List.of());
        when(inventarioRepository.findByObjetoMuseoIdAndEliminadoFalse(2L)).thenReturn(Optional.empty());
        when(fotoObjetoMuseoRepository.findByObjetoMuseoIdAndEliminadoFalse(2L)).thenReturn(List.of());
        when(reciboEscaneadoObjetoMuseoRepository.findFirstByObjetoMuseoIdAndEliminadoFalseOrderByFechaCargaDesc(2L)).thenReturn(Optional.empty());
        when(objetoDepositanteRepository.findFirstByObjetoMuseoIdAndEliminadoFalseOrderByIdAsc(2L)).thenReturn(Optional.empty());
        when(reciboIngresoObjetoRepository.save(any(ReciboIngresoObjeto.class))).thenAnswer(invocation -> {
            ReciboIngresoObjeto recibo = invocation.getArgument(0);
            recibo.setId(5L);
            return recibo;
        });

        ObjetoMuseoResponseDTO response = service.crear(new ObjetoMuseoRequestDTO("INV-2", "Carta", "Descripcion", null, null, null, null, null, null, 3L, CaracterRecepcionObjeto.DONACION, null));

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.numeroInventario()).isEqualTo("INV-2");
        assertThat(response.denominacionObjeto()).isEqualTo("Carta");
    }

    @Test
    void obtenerObjetoInexistenteLanzaResourceNotFoundException() {
        when(objetoMuseoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private ObjetoMuseo objeto(Long id, String numeroInventario) {
        ObjetoMuseo objeto = new ObjetoMuseo();
        objeto.setId(id);
        objeto.setNumeroInventario(numeroInventario);
        objeto.setDenominacionObjeto("Objeto");
        objeto.setEliminado(false);
        return objeto;
    }
}
