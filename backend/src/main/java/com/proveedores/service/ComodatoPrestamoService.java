package com.proveedores.service;

import com.proveedores.dto.ComodatoPrestamoResponseDTO;
import com.proveedores.dto.ConfigAlertasVencimientoDTO;
import com.proveedores.dto.EstadoVencimientoComodatoPrestamo;
import com.proveedores.dto.ObjetoVencimientoProximoResponseDTO;
import com.proveedores.entity.CaracterRecepcionObjeto;
import com.proveedores.entity.ConfiguracionSistema;
import com.proveedores.entity.Depositante;
import com.proveedores.entity.Inventario;
import com.proveedores.entity.ObjetoDepositante;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.TipoOperacionAuditoria;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.repository.ConfiguracionSistemaRepository;
import com.proveedores.repository.InventarioRepository;
import com.proveedores.repository.ObjetoDepositanteRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComodatoPrestamoService {

    private static final String CLAVE_DIAS_ALERTA = "comodatos_prestamos.dias_alerta";
    private static final int DIAS_ALERTA_DEFAULT = 14;
    private static final List<CaracterRecepcionObjeto> CARACTERES_GESTIONADOS = List.of(
            CaracterRecepcionObjeto.PRESTAMO,
            CaracterRecepcionObjeto.COMODATO
    );

    private final ObjetoDepositanteRepository objetoDepositanteRepository;
    private final InventarioRepository inventarioRepository;
    private final ConfiguracionSistemaRepository configuracionSistemaRepository;
    private final AuditoriaObjetoService auditoriaObjetoService;

    public ComodatoPrestamoService(
            ObjetoDepositanteRepository objetoDepositanteRepository,
            InventarioRepository inventarioRepository,
            ConfiguracionSistemaRepository configuracionSistemaRepository,
            AuditoriaObjetoService auditoriaObjetoService
    ) {
        this.objetoDepositanteRepository = objetoDepositanteRepository;
        this.inventarioRepository = inventarioRepository;
        this.configuracionSistemaRepository = configuracionSistemaRepository;
        this.auditoriaObjetoService = auditoriaObjetoService;
    }

    @Transactional(readOnly = true)
    public List<ComodatoPrestamoResponseDTO> listar() {
        int diasAlerta = obtenerDiasAlerta();
        LocalDate hoy = LocalDate.now();
        return objetoDepositanteRepository.findComodatosPrestamosActivosOrdenados(CARACTERES_GESTIONADOS).stream()
                .map(relacion -> toComodatoPrestamoResponse(relacion, hoy, diasAlerta))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ObjetoVencimientoProximoResponseDTO> listarVencimientosProximos(Integer dias) {
        int diasAlerta = dias == null ? obtenerDiasAlerta() : Math.max(0, dias);
        LocalDate hoy = LocalDate.now();
        LocalDate fechaHasta = hoy.plusDays(diasAlerta);
        return objetoDepositanteRepository
                .findByTipoDepositoInAndFechaVencimientoBetweenAndActivoTrueAndEliminadoFalseAndObjetoMuseoActivoTrueAndObjetoMuseoEliminadoFalseOrderByFechaVencimientoAsc(
                        CARACTERES_GESTIONADOS,
                        hoy,
                        fechaHasta
                )
                .stream()
                .map(relacion -> toVencimientoProximoResponse(relacion, hoy))
                .toList();
    }

    @Transactional
    public ComodatoPrestamoResponseDTO actualizarFechaVencimiento(Long objetoId, LocalDate fechaVencimiento) {
        return actualizarFechaVencimiento(objetoId, fechaVencimiento, null);
    }

    @Transactional
    public ComodatoPrestamoResponseDTO actualizarFechaVencimiento(Long objetoId, LocalDate fechaVencimiento, String operador) {
        ObjetoDepositante relacion = objetoDepositanteRepository.findRelacionActivaPorObjeto(objetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Objeto de museo no encontrado"));
        if (!esCaracterGestionado(relacion.getTipoDeposito())) {
            throw new BusinessException("Solo se puede actualizar la fecha de vencimiento de prestamos o comodatos");
        }
        LocalDate fechaIngreso = fechaIngreso(relacion);
        if (fechaVencimiento.isBefore(fechaIngreso)) {
            throw new BusinessException("La fecha de vencimiento no puede ser anterior a la fecha de ingreso");
        }
        LocalDate fechaAnterior = relacion.getFechaVencimiento();
        relacion.setFechaVencimiento(fechaVencimiento);
        ObjetoDepositante saved = objetoDepositanteRepository.save(relacion);
        auditoriaObjetoService.registrar(
                saved.getObjetoMuseo(),
                TipoOperacionAuditoria.MODIFICACION,
                "CAMBIO_FECHA_VENCIMIENTO",
                "Cambio de fecha de vencimiento de préstamo/comodato",
                "COMODATO_PRESTAMO",
                auditoriaObjetoService.mapOf("fechaVencimiento", fechaAnterior),
                auditoriaObjetoService.mapOf("fechaVencimiento", fechaVencimiento),
                operador
        );
        return toComodatoPrestamoResponse(saved, LocalDate.now(), obtenerDiasAlerta());
    }

    @Transactional(readOnly = true)
    public ConfigAlertasVencimientoDTO obtenerConfigAlertas() {
        return new ConfigAlertasVencimientoDTO(obtenerDiasAlerta());
    }

    @Transactional
    public ConfigAlertasVencimientoDTO actualizarConfigAlertas(ConfigAlertasVencimientoDTO dto) {
        validarDiasAlerta(dto.diasAnticipacion());
        ConfiguracionSistema config = configuracionSistemaRepository.findById(CLAVE_DIAS_ALERTA)
                .orElseGet(() -> nuevaConfiguracionDiasAlerta(dto.diasAnticipacion()));
        config.setValor(String.valueOf(dto.diasAnticipacion()));
        config.setDescripcion("Dias de anticipacion para alertas de comodatos y prestamos proximos a vencer");
        configuracionSistemaRepository.save(config);
        return new ConfigAlertasVencimientoDTO(dto.diasAnticipacion());
    }

    private ComodatoPrestamoResponseDTO toComodatoPrestamoResponse(ObjetoDepositante relacion, LocalDate hoy, int diasAlerta) {
        ObjetoMuseo objeto = relacion.getObjetoMuseo();
        Depositante depositante = relacion.getDepositante();
        LocalDate fechaVencimiento = relacion.getFechaVencimiento();
        Long diasRestantes = fechaVencimiento == null ? null : ChronoUnit.DAYS.between(hoy, fechaVencimiento);
        return new ComodatoPrestamoResponseDTO(
                objeto.getId(),
                objeto.getNumeroInventario(),
                objeto.getDenominacionObjeto(),
                depositante.getId(),
                depositante.getNombre(),
                relacion.getTipoDeposito(),
                fechaIngreso(relacion),
                fechaVencimiento,
                diasRestantes,
                estadoVencimiento(fechaVencimiento, hoy, diasAlerta)
        );
    }

    private ObjetoVencimientoProximoResponseDTO toVencimientoProximoResponse(ObjetoDepositante relacion, LocalDate hoy) {
        ObjetoMuseo objeto = relacion.getObjetoMuseo();
        Depositante depositante = relacion.getDepositante();
        return new ObjetoVencimientoProximoResponseDTO(
                objeto.getId(),
                objeto.getNumeroInventario(),
                objeto.getDenominacionObjeto(),
                depositante.getId(),
                depositante.getNombre(),
                relacion.getTipoDeposito(),
                relacion.getFechaVencimiento(),
                ChronoUnit.DAYS.between(hoy, relacion.getFechaVencimiento())
        );
    }

    private EstadoVencimientoComodatoPrestamo estadoVencimiento(LocalDate fechaVencimiento, LocalDate hoy, int diasAlerta) {
        if (fechaVencimiento == null) {
            return EstadoVencimientoComodatoPrestamo.VIGENTE;
        }
        if (fechaVencimiento.isBefore(hoy)) {
            return EstadoVencimientoComodatoPrestamo.VENCIDO;
        }
        if (!fechaVencimiento.isAfter(hoy.plusDays(diasAlerta))) {
            return EstadoVencimientoComodatoPrestamo.PROXIMO_A_VENCER;
        }
        return EstadoVencimientoComodatoPrestamo.VIGENTE;
    }

    private LocalDate fechaIngreso(ObjetoDepositante relacion) {
        return inventarioRepository.findByObjetoMuseoIdAndEliminadoFalse(relacion.getObjetoMuseo().getId())
                .map(Inventario::getFechaIngreso)
                .orElseGet(() -> relacion.getFechaDeposito() == null ? LocalDate.now() : relacion.getFechaDeposito());
    }

    private int obtenerDiasAlerta() {
        return configuracionSistemaRepository.findById(CLAVE_DIAS_ALERTA)
                .map(ConfiguracionSistema::getValor)
                .map(this::parseDiasAlerta)
                .orElse(DIAS_ALERTA_DEFAULT);
    }

    private int parseDiasAlerta(String valor) {
        try {
            int dias = Integer.parseInt(valor);
            validarDiasAlerta(dias);
            return dias;
        } catch (RuntimeException exception) {
            return DIAS_ALERTA_DEFAULT;
        }
    }

    private void validarDiasAlerta(int dias) {
        if (dias < 1 || dias > 365) {
            throw new BusinessException("Los dias de anticipacion deben estar entre 1 y 365");
        }
    }

    private ConfiguracionSistema nuevaConfiguracionDiasAlerta(int dias) {
        ConfiguracionSistema config = new ConfiguracionSistema();
        config.setClave(CLAVE_DIAS_ALERTA);
        config.setValor(String.valueOf(dias));
        return config;
    }

    private boolean esCaracterGestionado(CaracterRecepcionObjeto caracter) {
        return CARACTERES_GESTIONADOS.contains(caracter);
    }
}
