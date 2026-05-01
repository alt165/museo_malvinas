package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.proveedores.dto.ObjetoMuseoRequestDTO;
import com.proveedores.dto.ObjetoMuseoResponseDTO;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.ObjetoMuseoRepository;
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

    @InjectMocks
    private ObjetoMuseoService service;

    @Test
    void crearConNumeroInventarioDuplicadoLanzaBusinessException() {
        ObjetoMuseo existente = objeto(1L, "INV-1");
        when(objetoMuseoRepository.findByNumeroInventario("INV-1")).thenReturn(Optional.of(existente));

        ObjetoMuseoRequestDTO request = new ObjetoMuseoRequestDTO("INV-1", "Casco", "Equipo", null);

        assertThatThrownBy(() -> service.crear(request)).isInstanceOf(BusinessException.class);
        verify(objetoMuseoRepository, never()).save(any());
    }

    @Test
    void crearObjetoValidoDevuelveResponse() {
        when(objetoMuseoRepository.findByNumeroInventario("INV-2")).thenReturn(Optional.empty());
        when(objetoMuseoRepository.save(any(ObjetoMuseo.class))).thenAnswer(invocation -> {
            ObjetoMuseo entity = invocation.getArgument(0);
            entity.setId(2L);
            return entity;
        });

        ObjetoMuseoResponseDTO response = service.crear(new ObjetoMuseoRequestDTO("INV-2", "Carta", "Documento", "Descripcion"));

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.numeroInventario()).isEqualTo("INV-2");
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
        objeto.setNombre("Objeto");
        objeto.setEliminado(false);
        return objeto;
    }
}
