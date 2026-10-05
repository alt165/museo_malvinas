package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proveedores.dto.CargaRapidaObjetoRequestDTO;
import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.CategoriaObjeto;
import com.proveedores.entity.Depositante;
import com.proveedores.entity.EstadoConservacion;
import com.proveedores.entity.Inventario;
import com.proveedores.entity.MovimientoInventario;
import com.proveedores.entity.ObjetoDepositante;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.OrigenCargaObjeto;
import com.proveedores.entity.ReciboIngresoObjeto;
import com.proveedores.entity.TipoMovimientoInventario;
import com.proveedores.entity.Ubicacion;
import com.proveedores.entity.Usuario;
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
import com.proveedores.time.MuseoTime;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
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
    private UsuarioMovimientoService usuarioMovimientoService;

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
        CategoriaObjeto categoria = new CategoriaObjeto();
        categoria.setId(20L);
        categoria.setNombre("Categoria");
        categoria.setEliminado(false);
        when(categoriaObjetoRepository.findById(20L)).thenReturn(Optional.of(categoria));
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

        ObjetoMuseoResponseDTO response = service.crear(solicitudAltaCompleta(
                "Descripcion tecnica", "Papel", "10 cm", EstadoConservacion.BUENO, Set.of(20L), CaracterRecepcionObjeto.DONACION, null
        ));

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.numeroInventario()).matches("MMAS\\d{4}00002");
        assertThat(response.denominacionObjeto()).isEqualTo("Carta");
        ArgumentCaptor<ObjetoMuseo> objetoCaptor = ArgumentCaptor.forClass(ObjetoMuseo.class);
        verify(objetoMuseoRepository).save(objetoCaptor.capture());
        assertThat(objetoCaptor.getValue().getDatosCompletos()).isTrue();
    }

    @Test
    void altaCompletaSinDescripcionTecnicaEsRechazada() {
        assertThatThrownBy(() -> service.crear(solicitudAltaCompleta(
                null, "Papel", "10 cm", EstadoConservacion.BUENO, Set.of(20L), CaracterRecepcionObjeto.DONACION, null
        ))).isInstanceOf(BusinessException.class);
        verify(objetoMuseoRepository, never()).save(any());
    }

    @Test
    void altaCompletaSinMaterialesEsRechazada() {
        assertThatThrownBy(() -> service.crear(solicitudAltaCompleta(
                "Descripcion tecnica", null, "10 cm", EstadoConservacion.BUENO, Set.of(20L), CaracterRecepcionObjeto.DONACION, null
        ))).isInstanceOf(BusinessException.class);
        verify(objetoMuseoRepository, never()).save(any());
    }

    @Test
    void altaCompletaSinDimensionesEsRechazada() {
        assertThatThrownBy(() -> service.crear(solicitudAltaCompleta(
                "Descripcion tecnica", "Papel", null, EstadoConservacion.BUENO, Set.of(20L), CaracterRecepcionObjeto.DONACION, null
        ))).isInstanceOf(BusinessException.class);
        verify(objetoMuseoRepository, never()).save(any());
    }

    @Test
    void altaCompletaSinEstadoDeConservacionEsRechazada() {
        assertThatThrownBy(() -> service.crear(solicitudAltaCompleta(
                "Descripcion tecnica", "Papel", "10 cm", null, Set.of(20L), CaracterRecepcionObjeto.DONACION, null
        ))).isInstanceOf(BusinessException.class);
        verify(objetoMuseoRepository, never()).save(any());
    }

    @Test
    void altaCompletaSinCategoriasEsRechazada() {
        assertThatThrownBy(() -> service.crear(solicitudAltaCompleta(
                "Descripcion tecnica", "Papel", "10 cm", EstadoConservacion.BUENO, Set.of(), CaracterRecepcionObjeto.DONACION, null
        ))).isInstanceOf(BusinessException.class);
        verify(objetoMuseoRepository, never()).save(any());
    }

    @Test
    void altaCompletaPrestamoSinVencimientoEsRechazada() {
        assertThatThrownBy(() -> service.crear(solicitudAltaCompleta(
                "Descripcion tecnica", "Papel", "10 cm", EstadoConservacion.BUENO, Set.of(20L), CaracterRecepcionObjeto.PRESTAMO, null
        ))).isInstanceOf(BusinessException.class);
        verify(objetoMuseoRepository, never()).save(any());
    }

    @Test
    void cargaRapidaInicializaPrivacidadAntesDePersistir() {
        when(numeroInventarioRepository.siguienteCorrelativo(anyInt())).thenReturn(3);
        Depositante depositante = new Depositante();
        depositante.setId(4L);
        depositante.setEliminado(false);
        when(depositanteRepository.findById(4L)).thenReturn(Optional.of(depositante));
        when(objetoMuseoRepository.save(any(ObjetoMuseo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ubicacionRepository.findByNombreAndEliminadoFalse("Pre ingreso")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cargaRapida(
                new CargaRapidaObjetoRequestDTO(4L, "Objeto rapido", "Descripcion breve", CaracterRecepcionObjeto.DONACION, null),
                "operador-test"
        )).isInstanceOf(ResourceNotFoundException.class);

        ArgumentCaptor<ObjetoMuseo> objetoCaptor = ArgumentCaptor.forClass(ObjetoMuseo.class);
        verify(objetoMuseoRepository).save(objetoCaptor.capture());
        assertThat(objetoCaptor.getValue().getVisibilidades()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "depositante", VisibilidadCampo.PRIVADO,
                "ubicacion", VisibilidadCampo.PRIVADO
        ));
    }

    @Test
    void cargaRapidaRegistraUsuarioEnMovimientoDeIngreso() {
        when(numeroInventarioRepository.siguienteCorrelativo(anyInt())).thenReturn(3);
        Depositante depositante = new Depositante();
        depositante.setId(4L);
        depositante.setEliminado(false);
        Ubicacion preIngreso = ubicacion(7L, "Pre ingreso");
        Usuario operador = new Usuario();
        operador.setId(40L);
        operador.setNombre("operador-test");
        when(depositanteRepository.findById(4L)).thenReturn(Optional.of(depositante));
        when(ubicacionRepository.findByNombreAndEliminadoFalse("Pre ingreso")).thenReturn(Optional.of(preIngreso));
        when(usuarioMovimientoService.resolver("operador-test")).thenReturn(Optional.of(operador));
        when(objetoMuseoRepository.save(any(ObjetoMuseo.class))).thenAnswer(invocation -> {
            ObjetoMuseo entity = invocation.getArgument(0);
            entity.setId(12L);
            return entity;
        });
        when(inventarioRepository.save(any(Inventario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(movimientoInventarioRepository.save(any(MovimientoInventario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reciboIngresoObjetoRepository.save(any(ReciboIngresoObjeto.class))).thenAnswer(invocation -> {
            ReciboIngresoObjeto recibo = invocation.getArgument(0);
            recibo.setId(13L);
            return recibo;
        });
        when(objetoCategoriaRepository.findByObjetoMuseoIdAndEliminadoFalse(12L)).thenReturn(List.of());
        when(fotoObjetoMuseoRepository.findByObjetoMuseoIdAndEliminadoFalse(12L)).thenReturn(List.of());
        when(reciboEscaneadoObjetoMuseoRepository.findFirstByObjetoMuseoIdAndEliminadoFalseOrderByFechaCargaDesc(12L)).thenReturn(Optional.empty());
        when(objetoDepositanteRepository.findFirstByObjetoMuseoIdAndEliminadoFalseOrderByIdAsc(12L)).thenReturn(Optional.empty());

        LocalDateTime antes = MuseoTime.now();
        LocalDate fechaVencimiento = MuseoTime.today().plusDays(30);
        service.cargaRapida(
                new CargaRapidaObjetoRequestDTO(4L, "Objeto rapido", "Descripcion breve", CaracterRecepcionObjeto.PRESTAMO, fechaVencimiento),
                "operador-test"
        );
        LocalDateTime despues = MuseoTime.now();

        ArgumentCaptor<ObjetoMuseo> objetoCaptor = ArgumentCaptor.forClass(ObjetoMuseo.class);
        verify(objetoMuseoRepository).save(objetoCaptor.capture());
        assertThat(objetoCaptor.getValue().getFechaCargaRapida()).isBetween(antes, despues);

        ArgumentCaptor<Inventario> inventarioCaptor = ArgumentCaptor.forClass(Inventario.class);
        verify(inventarioRepository, times(2)).save(inventarioCaptor.capture());
        assertThat(inventarioCaptor.getAllValues())
                .extracting(Inventario::getFechaUltimoMovimiento)
                .allSatisfy(fecha -> assertThat(fecha).isBetween(antes, despues));

        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimientoInventario.INGRESO);
        assertThat(captor.getValue().getUsuario()).isSameAs(operador);
        assertThat(captor.getValue().getFecha()).isBetween(antes, despues);

        ArgumentCaptor<ObjetoDepositante> relacionCaptor = ArgumentCaptor.forClass(ObjetoDepositante.class);
        verify(objetoDepositanteRepository).save(relacionCaptor.capture());
        assertThat(relacionCaptor.getValue().getTipoDeposito()).isEqualTo(CaracterRecepcionObjeto.PRESTAMO);
        assertThat(relacionCaptor.getValue().getFechaVencimiento()).isEqualTo(fechaVencimiento);
    }

    @Test
    void cargaRapidaRechazaPrestamoSinFechaVencimiento() {
        assertThatThrownBy(() -> service.cargaRapida(new CargaRapidaObjetoRequestDTO(
                4L, "Objeto rapido", "Descripcion breve", CaracterRecepcionObjeto.PRESTAMO, null
        ), "operador-test")).isInstanceOf(BusinessException.class)
                .hasMessageContaining("fecha de vencimiento es obligatoria");
        verify(objetoMuseoRepository, never()).save(any());
    }

    @Test
    void cargaRapidaRechazaFechaVencimientoAnteriorAlIngreso() {
        assertThatThrownBy(() -> service.cargaRapida(new CargaRapidaObjetoRequestDTO(
                4L, "Objeto rapido", "Descripcion breve", CaracterRecepcionObjeto.COMODATO, MuseoTime.today().minusDays(1)
        ), "operador-test")).isInstanceOf(BusinessException.class)
                .hasMessageContaining("no puede ser anterior a la fecha de ingreso");
        verify(objetoMuseoRepository, never()).save(any());
    }

    @Test
    void completarCargaRapidaEnMismaUbicacionNoGeneraMovimiento() {
        ObjetoMuseo objeto = objetoRapidoPendiente(10L);
        Inventario inventario = inventarioActual(objeto, 7L, "Pre ingreso");
        prepararActualizacion(objeto, inventario);

        service.actualizar(10L, solicitudCompleta(7L), "operador-test");

        assertThat(objeto.getDatosCompletos()).isTrue();
        assertThat(objeto.getVisibilidades()).isEmpty();
        verify(movimientoInventarioRepository, never()).save(any(MovimientoInventario.class));
        verify(ubicacionRepository, never()).findById(any());
    }

    @Test
    void completarCargaRapidaEnOtraUbicacionRegistraCambioUbicacion() {
        ObjetoMuseo objeto = objetoRapidoPendiente(10L);
        Inventario inventario = inventarioActual(objeto, 7L, "Pre ingreso");
        Ubicacion destino = ubicacion(8L, "Sala principal");
        prepararActualizacion(objeto, inventario);
        when(ubicacionRepository.findById(8L)).thenReturn(Optional.of(destino));
        when(movimientoInventarioRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Usuario operador = new Usuario();
        operador.setId(40L);
        operador.setNombre("operador-test");
        when(usuarioMovimientoService.resolver("operador-test")).thenReturn(Optional.of(operador));

        LocalDateTime antes = MuseoTime.now();
        service.actualizar(10L, solicitudCompleta(8L), "operador-test");
        LocalDateTime despues = MuseoTime.now();

        assertThat(inventario.getUbicacion()).isSameAs(destino);
        ArgumentCaptor<MovimientoInventario> movimientoCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(movimientoCaptor.capture());
        assertThat(movimientoCaptor.getValue().getTipo()).isEqualTo(TipoMovimientoInventario.CAMBIO_UBICACION);
        assertThat(movimientoCaptor.getValue().getUbicacionOrigen().getId()).isEqualTo(7L);
        assertThat(movimientoCaptor.getValue().getUbicacionDestino().getId()).isEqualTo(8L);
        assertThat(movimientoCaptor.getValue().getFecha()).isBetween(antes, despues);
        assertThat(movimientoCaptor.getValue().getUsuario()).isSameAs(operador);
    }

    @Test
    void editarObjetoCompletoEnMismaUbicacionSoloActualizaPrivacidad() {
        ObjetoMuseo objeto = objeto(10L, "INV-10");
        objeto.setOrigenCarga(OrigenCargaObjeto.RAPIDA);
        objeto.setDatosCompletos(true);
        objeto.setVisibilidades(Map.of("ubicacion", VisibilidadCampo.PRIVADO));
        Inventario inventario = inventarioActual(objeto, 7L, "Sala actual");
        prepararActualizacion(objeto, inventario);

        service.actualizar(10L, solicitudCompleta(7L), "operador-test");

        assertThat(inventario.getUbicacion().getId()).isEqualTo(7L);
        assertThat(objeto.getVisibilidades()).doesNotContainKey("ubicacion");
        verify(ubicacionRepository, never()).findById(any());
        verify(movimientoInventarioRepository, never()).save(any(MovimientoInventario.class));
    }

    @Test
    void editarObjetoCompletoEnOtraUbicacionRegistraUnCambioYPersistePrivacidad() {
        ObjetoMuseo objeto = objeto(10L, "INV-10");
        objeto.setOrigenCarga(OrigenCargaObjeto.COMPLETA);
        objeto.setDatosCompletos(true);
        objeto.setVisibilidades(Map.of("ubicacion", VisibilidadCampo.PRIVADO));
        Inventario inventario = inventarioActual(objeto, 7L, "Sala actual");
        Ubicacion destino = ubicacion(8L, "Deposito principal");
        prepararActualizacion(objeto, inventario);
        when(ubicacionRepository.findById(8L)).thenReturn(Optional.of(destino));
        when(movimientoInventarioRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.actualizar(10L, solicitudCompleta(8L), "operador-test");

        assertThat(inventario.getUbicacion()).isSameAs(destino);
        assertThat(objeto.getVisibilidades()).doesNotContainKey("ubicacion");
        ArgumentCaptor<MovimientoInventario> movimientoCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(movimientoCaptor.capture());
        assertThat(movimientoCaptor.getValue().getTipo()).isEqualTo(TipoMovimientoInventario.CAMBIO_UBICACION);
        assertThat(movimientoCaptor.getValue().getUbicacionOrigen().getId()).isEqualTo(7L);
        assertThat(movimientoCaptor.getValue().getUbicacionDestino().getId()).isEqualTo(8L);
    }

    @Test
    void editarObjetoCompletoRechazaUbicacionInactiva() {
        ObjetoMuseo objeto = objeto(10L, "INV-10");
        objeto.setOrigenCarga(OrigenCargaObjeto.COMPLETA);
        objeto.setDatosCompletos(true);
        Inventario inventario = inventarioActual(objeto, 7L, "Sala actual");
        Ubicacion inactiva = ubicacion(8L, "Deposito inactivo");
        inactiva.setActivo(false);
        when(objetoMuseoRepository.findById(10L)).thenReturn(Optional.of(objeto));
        when(inventarioRepository.findByObjetoMuseoIdAndEliminadoFalse(10L)).thenReturn(Optional.of(inventario));
        when(ubicacionRepository.findById(8L)).thenReturn(Optional.of(inactiva));

        assertThatThrownBy(() -> service.actualizar(10L, solicitudCompleta(8L), "operador-test"))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(inventario.getUbicacion().getId()).isEqualTo(7L);
        verify(movimientoInventarioRepository, never()).save(any(MovimientoInventario.class));
    }

    @Test
    void completarCargaRapidaRechazaUbicacionInexistente() {
        ObjetoMuseo objeto = objetoRapidoPendiente(10L);
        Inventario inventario = inventarioActual(objeto, 7L, "Pre ingreso");
        when(objetoMuseoRepository.findById(10L)).thenReturn(Optional.of(objeto));
        when(inventarioRepository.findByObjetoMuseoIdAndEliminadoFalse(10L)).thenReturn(Optional.of(inventario));
        when(ubicacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(10L, solicitudCompleta(99L), "operador-test"))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(inventario.getUbicacion().getId()).isEqualTo(7L);
        verify(movimientoInventarioRepository, never()).save(any(MovimientoInventario.class));
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
        Ubicacion ubicacion = ubicacion(ubicacionId, ubicacionNombre);
        Inventario inventario = new Inventario();
        inventario.setObjetoMuseo(objeto);
        inventario.setUbicacion(ubicacion);
        inventario.setEliminado(false);
        return inventario;
    }

    private Ubicacion ubicacion(Long id, String nombre) {
        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setId(id);
        ubicacion.setNombre(nombre);
        ubicacion.setActivo(true);
        ubicacion.setEliminado(false);
        return ubicacion;
    }

    private ObjetoMuseo objetoRapidoPendiente(Long id) {
        ObjetoMuseo objeto = objeto(id, "INV-" + id);
        objeto.setOrigenCarga(OrigenCargaObjeto.RAPIDA);
        objeto.setDatosCompletos(false);
        objeto.setVisibilidades(Map.of("depositante", VisibilidadCampo.PRIVADO, "ubicacion", VisibilidadCampo.PRIVADO));
        return objeto;
    }

    private ObjetoMuseoRequestDTO solicitudCompleta(Long ubicacionId) {
        return new ObjetoMuseoRequestDTO(
                "INV-10",
                "Objeto completo",
                "Descripcion",
                "Descripcion tecnica",
                "Metal",
                "10 cm",
                EstadoConservacion.BUENO,
                Set.of(20L),
                ubicacionId,
                30L,
                CaracterRecepcionObjeto.DONACION,
                null
        );
    }

    private ObjetoMuseoRequestDTO solicitudAltaCompleta(
            String descripcionTecnica,
            String materiales,
            String dimensiones,
            EstadoConservacion estadoConservacion,
            Set<Long> categoriaIds,
            CaracterRecepcionObjeto caracterRecepcion,
            java.time.LocalDate fechaVencimiento
    ) {
        return new ObjetoMuseoRequestDTO(
                "INV-2",
                "Carta",
                "Descripcion",
                descripcionTecnica,
                materiales,
                dimensiones,
                estadoConservacion,
                categoriaIds,
                null,
                3L,
                caracterRecepcion,
                fechaVencimiento
        );
    }

    private void prepararActualizacion(ObjetoMuseo objeto, Inventario inventario) {
        CategoriaObjeto categoria = new CategoriaObjeto();
        categoria.setId(20L);
        categoria.setNombre("Categoria");
        categoria.setEliminado(false);
        Depositante depositante = new Depositante();
        depositante.setId(30L);
        depositante.setNombre("Depositante");
        depositante.setEliminado(false);

        when(objetoMuseoRepository.findById(objeto.getId())).thenReturn(Optional.of(objeto));
        when(objetoMuseoRepository.save(any(ObjetoMuseo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventarioRepository.findByObjetoMuseoIdAndEliminadoFalse(objeto.getId())).thenReturn(Optional.of(inventario));
        when(objetoCategoriaRepository.findByObjetoMuseoIdAndEliminadoFalse(objeto.getId())).thenReturn(List.of());
        when(categoriaObjetoRepository.findById(20L)).thenReturn(Optional.of(categoria));
        when(depositanteRepository.findById(30L)).thenReturn(Optional.of(depositante));
        when(fotoObjetoMuseoRepository.findByObjetoMuseoIdAndEliminadoFalse(objeto.getId())).thenReturn(List.of());
        when(reciboEscaneadoObjetoMuseoRepository.findFirstByObjetoMuseoIdAndEliminadoFalseOrderByFechaCargaDesc(objeto.getId()))
                .thenReturn(Optional.empty());
        when(objetoDepositanteRepository.findFirstByObjetoMuseoIdAndEliminadoFalseOrderByIdAsc(objeto.getId()))
                .thenReturn(Optional.empty());
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
