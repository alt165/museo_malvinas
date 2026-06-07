package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.proveedores.dto.ConfigAlertasVencimientoDTO;
import com.proveedores.dto.EstadoVencimientoComodatoPrestamo;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.ConfiguracionSistema;
import com.proveedores.entity.Depositante;
import com.proveedores.entity.Inventario;
import com.proveedores.entity.ObjetoDepositante;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.exception.BusinessException;
import com.proveedores.repository.ConfiguracionSistemaRepository;
import com.proveedores.repository.InventarioRepository;
import com.proveedores.repository.ObjetoDepositanteRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ComodatoPrestamoServiceTest {

    @Mock
    private ObjetoDepositanteRepository objetoDepositanteRepository;

    @Mock
    private InventarioRepository inventarioRepository;

    @Mock
    private ConfiguracionSistemaRepository configuracionSistemaRepository;

    @InjectMocks
    private ComodatoPrestamoService service;

    @Test
    void listarCalculaEstadoSegunConfigDeAlertas() {
        LocalDate hoy = LocalDate.now();
        ObjetoDepositante vencido = relacion(1L, "INV-1", CaracterRecepcionObjeto.PRESTAMO, hoy.minusDays(1));
        ObjetoDepositante proximo = relacion(2L, "INV-2", CaracterRecepcionObjeto.COMODATO, hoy.plusDays(10));
        ObjetoDepositante vigente = relacion(3L, "INV-3", CaracterRecepcionObjeto.PRESTAMO, hoy.plusDays(30));
        ObjetoDepositante sinFecha = relacion(4L, "INV-4", CaracterRecepcionObjeto.COMODATO, null);
        when(configuracionSistemaRepository.findById("comodatos_prestamos.dias_alerta")).thenReturn(Optional.of(config("14")));
        when(objetoDepositanteRepository.findComodatosPrestamosActivosOrdenados(List.of(CaracterRecepcionObjeto.PRESTAMO, CaracterRecepcionObjeto.COMODATO)))
                .thenReturn(List.of(vencido, proximo, vigente, sinFecha));

        var response = service.listar();

        assertThat(response).extracting("estadoVencimiento")
                .containsExactly(
                        EstadoVencimientoComodatoPrestamo.VENCIDO,
                        EstadoVencimientoComodatoPrestamo.PROXIMO_A_VENCER,
                        EstadoVencimientoComodatoPrestamo.VIGENTE,
                        EstadoVencimientoComodatoPrestamo.VIGENTE
                );
        assertThat(response.get(0).diasRestantes()).isEqualTo(-1);
        assertThat(response.get(3).diasRestantes()).isNull();
    }


    @Test
    void listarVencimientosProximosDevuelvePrestamosYComodatosConDiasRestantes() {
        LocalDate hoy = LocalDate.now();
        ObjetoDepositante prestamo = relacion(10L, "INV-10", CaracterRecepcionObjeto.PRESTAMO, hoy.plusDays(3));
        ObjetoDepositante comodato = relacion(11L, "INV-11", CaracterRecepcionObjeto.COMODATO, hoy.plusDays(14));
        when(objetoDepositanteRepository.findByTipoDepositoInAndFechaVencimientoBetweenAndActivoTrueAndEliminadoFalseAndObjetoMuseoActivoTrueAndObjetoMuseoEliminadoFalseOrderByFechaVencimientoAsc(
                List.of(CaracterRecepcionObjeto.PRESTAMO, CaracterRecepcionObjeto.COMODATO),
                hoy,
                hoy.plusDays(14)
        )).thenReturn(List.of(prestamo, comodato));

        var response = service.listarVencimientosProximos(14);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).id()).isEqualTo(10L);
        assertThat(response.get(0).numeroInventario()).isEqualTo("INV-10");
        assertThat(response.get(0).depositanteNombre()).isEqualTo("Depositante");
        assertThat(response.get(0).caracterRecepcion()).isEqualTo(CaracterRecepcionObjeto.PRESTAMO);
        assertThat(response.get(0).fechaVencimiento()).isEqualTo(hoy.plusDays(3));
        assertThat(response.get(0).diasRestantes()).isEqualTo(3);
        assertThat(response.get(1).diasRestantes()).isEqualTo(14);
    }

    @Test
    void actualizarFechaVencimientoRechazaCaracterNoGestionado() {
        ObjetoDepositante relacion = relacion(1L, "INV-1", CaracterRecepcionObjeto.DONACION, LocalDate.now().plusDays(10));
        when(objetoDepositanteRepository.findRelacionActivaPorObjeto(1L)).thenReturn(Optional.of(relacion));

        assertThatThrownBy(() -> service.actualizarFechaVencimiento(1L, LocalDate.now().plusDays(20)))
                .isInstanceOf(BusinessException.class);
        verify(objetoDepositanteRepository, never()).save(any());
    }

    @Test
    void actualizarFechaVencimientoRechazaFechaAnteriorAIngreso() {
        LocalDate fechaIngreso = LocalDate.now();
        ObjetoDepositante relacion = relacion(1L, "INV-1", CaracterRecepcionObjeto.PRESTAMO, fechaIngreso.plusDays(10));
        Inventario inventario = new Inventario();
        inventario.setFechaIngreso(fechaIngreso);
        when(objetoDepositanteRepository.findRelacionActivaPorObjeto(1L)).thenReturn(Optional.of(relacion));
        when(inventarioRepository.findByObjetoMuseoIdAndEliminadoFalse(1L)).thenReturn(Optional.of(inventario));

        assertThatThrownBy(() -> service.actualizarFechaVencimiento(1L, fechaIngreso.minusDays(1)))
                .isInstanceOf(BusinessException.class);
        verify(objetoDepositanteRepository, never()).save(any());
    }

    @Test
    void actualizarConfigAlertasValidaRango() {
        assertThatThrownBy(() -> service.actualizarConfigAlertas(new ConfigAlertasVencimientoDTO(0)))
                .isInstanceOf(BusinessException.class);
    }

    private ObjetoDepositante relacion(Long objetoId, String numeroInventario, CaracterRecepcionObjeto caracter, LocalDate fechaVencimiento) {
        ObjetoMuseo objeto = new ObjetoMuseo();
        objeto.setId(objetoId);
        objeto.setNumeroInventario(numeroInventario);
        objeto.setDenominacionObjeto("Objeto " + numeroInventario);
        objeto.setActivo(true);
        objeto.setEliminado(false);

        Depositante depositante = new Depositante();
        depositante.setId(10L + objetoId);
        depositante.setNombre("Depositante");

        ObjetoDepositante relacion = new ObjetoDepositante();
        relacion.setObjetoMuseo(objeto);
        relacion.setDepositante(depositante);
        relacion.setTipoDeposito(caracter);
        relacion.setFechaDeposito(LocalDate.now());
        relacion.setFechaVencimiento(fechaVencimiento);
        relacion.setActivo(true);
        relacion.setEliminado(false);
        return relacion;
    }

    private ConfiguracionSistema config(String valor) {
        ConfiguracionSistema config = new ConfiguracionSistema();
        config.setClave("comodatos_prestamos.dias_alerta");
        config.setValor(valor);
        return config;
    }
}
