package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.proveedores.dto.VeteranoRequestDTO;
import com.proveedores.entity.Fuerza;
import com.proveedores.entity.Veterano;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.VeteranoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VeteranoServiceTest {

    @Mock
    private VeteranoRepository veteranoRepository;

    @InjectMocks
    private VeteranoService service;

    @Test
    void crearVeteranoDevuelveNombreCompleto() {
        when(veteranoRepository.save(any(Veterano.class))).thenAnswer(invocation -> {
            Veterano entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        var response = service.crear(new VeteranoRequestDTO("Juan", "Perez", Fuerza.EJERCITO, null, null, "Historia"));

        assertThat(response.nombreCompleto()).isEqualTo("Juan Perez");
        assertThat(response.fuerza()).isEqualTo(Fuerza.EJERCITO);
    }

    @Test
    void obtenerVeteranoInexistenteLanzaResourceNotFoundException() {
        when(veteranoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
