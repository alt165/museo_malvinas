package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.proveedores.dto.RelacionObjetoRequestDTO;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.RelacionObjeto;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.repository.RelacionObjetoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RelacionObjetoServiceTest {

    @Mock
    private RelacionObjetoRepository relacionObjetoRepository;
    @Mock
    private ObjetoMuseoRepository objetoMuseoRepository;

    @InjectMocks
    private RelacionObjetoService service;

    @Test
    void crearRelacionValidaEntidadesRelacionadas() {
        when(objetoMuseoRepository.findById(1L)).thenReturn(Optional.of(objeto(1L, "Origen")));
        when(objetoMuseoRepository.findById(2L)).thenReturn(Optional.of(objeto(2L, "Destino")));
        when(relacionObjetoRepository.findByObjetoOrigenIdAndObjetoDestinoIdAndTipoRelacionAndEliminadoFalse(1L, 2L, "pertenece a"))
                .thenReturn(Optional.empty());
        when(relacionObjetoRepository.save(any(RelacionObjeto.class))).thenAnswer(invocation -> {
            RelacionObjeto entity = invocation.getArgument(0);
            entity.setId(5L);
            return entity;
        });

        var response = service.crear(new RelacionObjetoRequestDTO(1L, 2L, "pertenece a", null));

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.objetoOrigenNombre()).isEqualTo("Origen");
        assertThat(response.objetoDestinoNombre()).isEqualTo("Destino");
    }

    @Test
    void crearConDestinoInexistenteLanzaResourceNotFoundException() {
        when(objetoMuseoRepository.findById(1L)).thenReturn(Optional.of(objeto(1L, "Origen")));
        when(objetoMuseoRepository.findById(2L)).thenReturn(Optional.empty());
        when(relacionObjetoRepository.findByObjetoOrigenIdAndObjetoDestinoIdAndTipoRelacionAndEliminadoFalse(1L, 2L, "pertenece a"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new RelacionObjetoRequestDTO(1L, 2L, "pertenece a", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private ObjetoMuseo objeto(Long id, String nombre) {
        ObjetoMuseo objeto = new ObjetoMuseo();
        objeto.setId(id);
        objeto.setDenominacionObjeto(nombre);
        objeto.setNumeroInventario("INV-" + id);
        objeto.setEliminado(false);
        return objeto;
    }
}
