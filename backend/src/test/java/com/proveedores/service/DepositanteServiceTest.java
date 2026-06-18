package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.proveedores.dto.DepositanteResponseDTO;
import com.proveedores.entity.Depositante;
import com.proveedores.entity.TipoDepositante;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.DepositanteRepository;
import com.proveedores.repository.ObjetoDepositanteRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
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

    @InjectMocks
    private DepositanteService service;

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
