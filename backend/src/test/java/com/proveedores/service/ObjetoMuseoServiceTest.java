package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.Depositante;
import com.proveedores.entity.Inventario;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.ReciboIngresoObjeto;
import com.proveedores.entity.Ubicacion;
import com.proveedores.entity.VisibilidadCampo;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.CategoriaObjetoRepository;
import com.proveedores.repository.DepositanteRepository;
import com.proveedores.repository.EmbargoObjetoRepository;
import com.proveedores.repository.FotoObjetoMuseoRepository;
import com.proveedores.repository.InventarioRepository;
import com.proveedores.repository.MovimientoInventarioRepository;
import com.proveedores.repository.NumeroInventarioRepository;
import com.proveedores.repository.ObjetoCategoriaRepository;
import com.proveedores.repository.ObjetoDepositanteRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.repository.ReciboEscaneadoObjetoMuseoRepository;
import com.proveedores.repository.ReciboIngresoObjetoRepository;
import com.proveedores.repository.UbicacionRepository;
import com.proveedores.repository.UsuarioRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ObjetoMuseoServiceTest {

    @Mock
    private ObjetoMuseoRepository objetoMuseoRepository;

    @Mock
    private NumeroInventarioRepository numeroInventarioRepository;

    @Mock
    private CategoriaObjetoRepository categoriaObjetoRepository;

    @Mock
    private ObjetoCategoriaRepository objetoCategoriaRepository;

    @Mock
    private DepositanteRepository depositanteRepository;

    @Mock
    private EmbargoObjetoRepository embargoObjetoRepository;

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

    @Mock
    private AuditoriaObjetoService auditoriaObjetoService;

    @Mock
    private DetalleConservacionService detalleConservacionService;

    @InjectMocks
    private ObjetoMuseoService service;

    @AfterEach
    void limpiarContextoSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void crearSinDepositanteLanzaBusinessException() {
        ObjetoMuseoRequestDTO request = new ObjetoMuseoRequestDTO("INV-1", "Casco", null, null, null, null, null, null);

        assertThatThrownBy(() -> service.crear(request)).isInstanceOf(BusinessException.class);
        verify(objetoMuseoRepository, never()).save(any());
    }

    @Test
    void crearObjetoValidoDevuelveResponse() {
        when(numeroInventarioRepository.siguienteCorrelativo(anyInt())).thenReturn(2);
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
        assertThat(response.numeroInventario()).matches("MMAS\\d{4}00002");
        assertThat(response.denominacionObjeto()).isEqualTo("Carta");
    }

    @Test
    void obtenerObjetoInexistenteLanzaResourceNotFoundException() {
        when(objetoMuseoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerPorIdIncluyeUbicacionActualPublicaParaViewer() {
        ObjetoMuseo objeto = objeto(1L, "INV-1");
        Inventario inventario = inventarioActual(objeto, 7L, "Sala principal");
        autenticarComo("ROLE_VIEWER");
        when(objetoMuseoRepository.findById(1L)).thenReturn(Optional.of(objeto));
        when(inventarioRepository.findByObjetoMuseoIdAndEliminadoFalse(1L)).thenReturn(Optional.of(inventario));

        ObjetoMuseoResponseDTO response = service.obtenerPorId(1L);

        assertThat(response.ubicacionVisible()).isTrue();
        assertThat(response.ubicacionId()).isEqualTo(7L);
        assertThat(response.ubicacionNombre()).isEqualTo("Sala principal");
    }

    @Test
    void obtenerPorIdNoExponeUbicacionPrivadaParaViewer() throws Exception {
        ObjetoMuseo objeto = objeto(1L, "INV-1");
        objeto.setVisibilidades(Map.of("ubicacion", VisibilidadCampo.PRIVADO));
        Inventario inventario = inventarioActual(objeto, 7L, "Deposito reservado");
        autenticarComo("ROLE_VIEWER");
        when(objetoMuseoRepository.findById(1L)).thenReturn(Optional.of(objeto));
        when(inventarioRepository.findByObjetoMuseoIdAndEliminadoFalse(1L)).thenReturn(Optional.of(inventario));

        ObjetoMuseoResponseDTO response = service.obtenerPorId(1L);

        assertThat(response.ubicacionVisible()).isFalse();
        assertThat(response.ubicacionId()).isNull();
        assertThat(response.ubicacionNombre()).isNull();
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(response);
        assertThat(json).doesNotContain("ubicacionId", "ubicacionNombre", "Deposito reservado");
    }

    @Test
    void obtenerPorIdExponeUbicacionPrivadaParaOperator() {
        ObjetoMuseo objeto = objeto(1L, "INV-1");
        objeto.setVisibilidades(Map.of("ubicacion", VisibilidadCampo.PRIVADO));
        Inventario inventario = inventarioActual(objeto, 7L, "Deposito reservado");
        autenticarComo("ROLE_OPERATOR");
        when(objetoMuseoRepository.findById(1L)).thenReturn(Optional.of(objeto));
        when(inventarioRepository.findByObjetoMuseoIdAndEliminadoFalse(1L)).thenReturn(Optional.of(inventario));

        ObjetoMuseoResponseDTO response = service.obtenerPorId(1L);

        assertThat(response.ubicacionVisible()).isTrue();
        assertThat(response.ubicacionId()).isEqualTo(7L);
        assertThat(response.ubicacionNombre()).isEqualTo("Deposito reservado");
    }

    private void autenticarComo(String authority) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "usuario",
                null,
                List.of(new SimpleGrantedAuthority(authority))
        ));
    }

    private Inventario inventarioActual(ObjetoMuseo objeto, Long ubicacionId, String ubicacionNombre) {
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setId(ubicacionId);
        ubicacion.setNombre(ubicacionNombre);
        Inventario inventario = new Inventario();
        inventario.setObjetoMuseo(objeto);
        inventario.setUbicacion(ubicacion);
        inventario.setEliminado(false);
        return inventario;
    }

    private ObjetoMuseo objeto(Long id, String numeroInventario) {
        ObjetoMuseo objeto = new ObjetoMuseo();
        objeto.setId(id);
        objeto.setNumeroInventario(numeroInventario);
        objeto.setDenominacionObjeto("Objeto");
        objeto.setActivo(true);
        objeto.setEliminado(false);
        return objeto;
    }
}
