package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;

import com.proveedores.dto.DepositanteResponseDTO;
import com.proveedores.dto.DepositanteRequestDTO;
import com.proveedores.entity.Depositante;
import com.proveedores.entity.TipoDepositante;
import com.proveedores.exception.ConflictException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.DepositanteRepository;
import com.proveedores.repository.ObjetoDepositanteRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepositanteServiceTest {

    @Mock
    private DepositanteRepository depositanteRepository;

    @Mock
    private ObjetoDepositanteRepository objetoDepositanteRepository;

    @Mock
    private ObjetoMuseoService objetoMuseoService;
    @Mock
    private AuditoriaObjetoService auditoriaService;

    @InjectMocks
    private DepositanteService service;

    @Test
    void altaPersonaConDniNuevoCreaRegistro() {
        when(depositanteRepository.findPrimeroPorDniNormalizado("12345678")).thenReturn(Optional.empty());
        when(depositanteRepository.save(any(Depositante.class))).thenAnswer(invocation -> {
            Depositante saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        DepositanteResponseDTO response = service.crear(request(TipoDepositante.PERSONA, "12.345.678", null, "Actual"));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.dni()).isEqualTo("12.345.678");
    }

    @Test
    void altaInstitucionConCuitNuevoCreaRegistro() {
        when(depositanteRepository.findPrimeroPorCuitNormalizado("30123456789")).thenReturn(Optional.empty());
        when(depositanteRepository.save(any(Depositante.class))).thenAnswer(invocation -> {
            Depositante saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        DepositanteResponseDTO response = service.crear(request(TipoDepositante.INSTITUCION, null, "30-12345678-9", "Actual"));

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.cuit()).isEqualTo("30-12345678-9");
    }

    @Test
    void altaConDniActivoDevuelveDuplicado() {
        Depositante actual = depositante(12L, "Existente", TipoDepositante.PERSONA);
        when(depositanteRepository.findPrimeroPorDniNormalizado("12345678")).thenReturn(Optional.of(actual));

        assertThatThrownBy(() -> service.crear(request(TipoDepositante.PERSONA, "12345678", null, "Otro")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe un depositante activo con este DNI");
    }

    @Test
    void altaConCuitActivoDevuelveDuplicado() {
        Depositante actual = depositante(13L, "Institucion actual", TipoDepositante.INSTITUCION);
        when(depositanteRepository.findPrimeroPorCuitNormalizado("30123456789")).thenReturn(Optional.of(actual));

        assertThatThrownBy(() -> service.crear(request(TipoDepositante.INSTITUCION, null, "30-12345678-9", "Otro")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe un depositante activo con este CUIT");
    }

    @Test
    void altaConDniEliminadoDevuelveConflictoIdentificable() {
        Depositante eliminado = depositante(14L, "Anterior", TipoDepositante.PERSONA);
        eliminado.setEliminado(true);
        when(depositanteRepository.findPrimeroPorDniNormalizado("12345678")).thenReturn(Optional.of(eliminado));

        assertThatThrownBy(() -> service.crear(request(TipoDepositante.PERSONA, "12345678", null, "Nuevo")))
                .isInstanceOfSatisfying(ConflictException.class, conflict -> {
                    assertThat(conflict.getCode()).isEqualTo("DEPOSITANTE_ELIMINADO");
                    assertThat(conflict.getDepositanteId()).isEqualTo(14L);
                    assertThat(conflict.getTipoIdentificacion()).isEqualTo("DNI");
                });
    }

    @Test
    void altaConCuitEliminadoDevuelveConflictoIdentificable() {
        Depositante eliminado = depositante(15L, "Institucion anterior", TipoDepositante.INSTITUCION);
        eliminado.setEliminado(true);
        when(depositanteRepository.findPrimeroPorCuitNormalizado("30123456789")).thenReturn(Optional.of(eliminado));

        assertThatThrownBy(() -> service.crear(request(TipoDepositante.INSTITUCION, null, "30-12345678-9", "Nuevo")))
                .isInstanceOfSatisfying(ConflictException.class, conflict -> {
                    assertThat(conflict.getCode()).isEqualTo("DEPOSITANTE_ELIMINADO");
                    assertThat(conflict.getDepositanteId()).isEqualTo(15L);
                    assertThat(conflict.getTipoIdentificacion()).isEqualTo("CUIT");
                });
    }

    @Test
    void restaurarConservaIdDatosYNoCreaOtroRegistro() {
        Depositante eliminado = depositante(16L, "Nombre histórico", TipoDepositante.PERSONA);
        eliminado.setDni("12.345.678");
        eliminado.setContacto("historico@example.com");
        eliminado.setObservaciones("Teléfono anterior y relaciones preservadas");
        eliminado.setActivo(false);
        eliminado.setEliminado(true);
        eliminado.setFechaEliminacion(LocalDateTime.now());
        when(depositanteRepository.findById(16L)).thenReturn(Optional.of(eliminado));
        when(depositanteRepository.save(eliminado)).thenReturn(eliminado);

        DepositanteResponseDTO response = service.restaurar(16L);

        assertThat(response.id()).isEqualTo(16L);
        assertThat(response.nombre()).isEqualTo("Nombre histórico");
        assertThat(response.dni()).isEqualTo("12.345.678");
        assertThat(response.contacto()).isEqualTo("historico@example.com");
        assertThat(response.observaciones()).isEqualTo("Teléfono anterior y relaciones preservadas");
        assertThat(eliminado.getActivo()).isTrue();
        assertThat(eliminado.getEliminado()).isFalse();
        assertThat(eliminado.getFechaEliminacion()).isNull();
        verify(depositanteRepository).save(eliminado);
    }

    private DepositanteRequestDTO request(TipoDepositante tipo, String dni, String cuit, String nombre) {
        return new DepositanteRequestDTO(nombre, tipo, null, dni, cuit, "Formulario actual");
    }

    @Test
    void buscarPorDniExistenteDevuelveDepositante() {
        Depositante depositante = depositante(1L, "Juan Perez", TipoDepositante.PERSONA);
        depositante.setDni("12.345.678");
        when(depositanteRepository.findActivoByIdentificacionNormalizada("12345678"))
                .thenReturn(Optional.of(depositante));

        DepositanteResponseDTO response = service.buscarPorIdentificacion("12.345.678");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.dni()).isEqualTo("12.345.678");
    }

    @Test
    void buscarPorCuitExistenteDevuelveDepositante() {
        Depositante depositante = depositante(2L, "Asociacion Test", TipoDepositante.INSTITUCION);
        depositante.setCuit("30-12345678-9");
        when(depositanteRepository.findActivoByIdentificacionNormalizada("30123456789"))
                .thenReturn(Optional.of(depositante));

        DepositanteResponseDTO response = service.buscarPorIdentificacion("30-12345678-9");

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.cuit()).isEqualTo("30-12345678-9");
    }

    @Test
    void buscarSinPuntosNiGuionesUsaIdentificacionNormalizada() {
        Depositante depositante = depositante(3L, "Maria Gomez", TipoDepositante.PERSONA);
        depositante.setDni("22.333.444");
        when(depositanteRepository.findActivoByIdentificacionNormalizada("22333444"))
                .thenReturn(Optional.of(depositante));

        DepositanteResponseDTO response = service.buscarPorIdentificacion("22333444");

        assertThat(response.id()).isEqualTo(3L);
    }

    @Test
    void buscarNoEncontradoLanzaResourceNotFoundException() {
        when(depositanteRepository.findActivoByIdentificacionNormalizada("99999999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorIdentificacion("99.999.999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Depositante no encontrado");
    }

    @Test
    void buscarPorNombreDevuelveCoincidenciasParcialesCaseInsensitive() {
        Depositante juan = depositante(4L, "Juan Perez", TipoDepositante.PERSONA);
        when(depositanteRepository.findByNombreContainingIgnoreCaseAndEliminadoFalse("JUAN"))
                .thenReturn(List.of(juan));
        when(depositanteRepository.findAll()).thenReturn(List.of(juan));

        List<DepositanteResponseDTO> response = service.buscarPorNombre("JUAN");

        assertThat(response).extracting(DepositanteResponseDTO::id).containsExactly(4L);
    }

    @Test
    void buscarPorNombreNormalizaAcentos() {
        Depositante juan = depositante(5L, "Juan Pérez", TipoDepositante.PERSONA);
        when(depositanteRepository.findByNombreContainingIgnoreCaseAndEliminadoFalse("Perez"))
                .thenReturn(List.of());
        when(depositanteRepository.findAll()).thenReturn(List.of(juan));

        List<DepositanteResponseDTO> response = service.buscarPorNombre("Perez");

        assertThat(response).extracting(DepositanteResponseDTO::id).containsExactly(5L);
    }

    @Test
    void buscarPorNombreVacioLanzaBusinessException() {
        assertThatThrownBy(() -> service.buscarPorNombre("  "))
                .isInstanceOf(com.proveedores.exception.BusinessException.class)
                .hasMessage("El nombre de busqueda es obligatorio");
    }

    @Test
    void dniYCuitSonString() throws NoSuchFieldException {
        Field dni = Depositante.class.getDeclaredField("dni");
        Field cuit = Depositante.class.getDeclaredField("cuit");

        assertThat(dni.getType()).isEqualTo(String.class);
        assertThat(cuit.getType()).isEqualTo(String.class);
    }

    private Depositante depositante(Long id, String nombre, TipoDepositante tipo) {
        Depositante depositante = new Depositante();
        depositante.setId(id);
        depositante.setNombre(nombre);
        depositante.setTipo(tipo);
        depositante.setEliminado(false);
        return depositante;
    }
}
