package com.proveedores.service;

import com.proveedores.dto.InventarioRequestDTO;
import com.proveedores.dto.InventarioResponseDTO;
import com.proveedores.entity.EstadoInventario;
import com.proveedores.entity.Inventario;
import com.proveedores.entity.MovimientoInventario;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.TipoMovimientoInventario;
import com.proveedores.entity.Ubicacion;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.InventarioMapper;
import com.proveedores.repository.InventarioRepository;
import com.proveedores.repository.MovimientoInventarioRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.repository.UbicacionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventarioService {

    private static final Logger log = LoggerFactory.getLogger(InventarioService.class);

    private final InventarioRepository inventarioRepository;
    private final ObjetoMuseoRepository objetoMuseoRepository;
    private final UbicacionRepository ubicacionRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;

    public InventarioService(
            InventarioRepository inventarioRepository,
            ObjetoMuseoRepository objetoMuseoRepository,
            UbicacionRepository ubicacionRepository,
            MovimientoInventarioRepository movimientoInventarioRepository
    ) {
        this.inventarioRepository = inventarioRepository;
        this.objetoMuseoRepository = objetoMuseoRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
    }

    @Transactional
    public InventarioResponseDTO crear(InventarioRequestDTO dto) {
        ObjetoMuseo objeto = buscarObjeto(dto.objetoMuseoId());
        Ubicacion ubicacion = buscarUbicacion(dto.ubicacionId());
        Inventario entity = InventarioMapper.toEntity(dto);
        entity.setObjetoMuseo(objeto);
        entity.setUbicacion(ubicacion);
        entity.setFechaUltimoMovimiento(LocalDateTime.now());
        Inventario saved = inventarioRepository.save(entity);
        registrarMovimiento(saved, TipoMovimientoInventario.INGRESO, null, ubicacion);
        log.info("event=inventario.created inventarioId={} objetoMuseoId={} ubicacionId={} estado={}", saved.getId(), objeto.getId(), ubicacion.getId(), saved.getEstado());
        return InventarioMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public InventarioResponseDTO obtenerPorId(Long id) {
        return InventarioMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<InventarioResponseDTO> listar() {
        return inventarioRepository.findAll().stream().filter(e -> !e.getEliminado()).map(InventarioMapper::toResponse).toList();
    }

    @Transactional
    public InventarioResponseDTO actualizar(Long id, InventarioRequestDTO dto) {
        Inventario entity = buscarActivo(id);
        ObjetoMuseo objeto = buscarObjeto(dto.objetoMuseoId());
        Ubicacion nuevaUbicacion = buscarUbicacion(dto.ubicacionId());
        Ubicacion ubicacionAnterior = entity.getUbicacion();
        EstadoInventario estadoAnterior = entity.getEstado();
        boolean cambioUbicacion = !Objects.equals(ubicacionAnterior.getId(), nuevaUbicacion.getId());
        boolean cambioEstado = estadoAnterior != dto.estado();

        entity.setObjetoMuseo(objeto);
        entity.setUbicacion(nuevaUbicacion);
        entity.setEstado(dto.estado());
        entity.setEstadoConservacion(dto.estadoConservacion());
        entity.setFechaIngreso(dto.fechaIngreso());
        entity.setFechaSalida(dto.fechaSalida());
        entity.setFechaUltimoMovimiento(LocalDateTime.now());
        entity.setObservaciones(dto.observaciones());
        Inventario saved = inventarioRepository.save(entity);

        if (cambioUbicacion || cambioEstado) {
            registrarMovimiento(saved, resolverTipoMovimiento(estadoAnterior, dto.estado(), cambioUbicacion), ubicacionAnterior, nuevaUbicacion);
            log.info(
                    "event=inventario.state_changed inventarioId={} objetoMuseoId={} estadoAnterior={} estadoNuevo={} ubicacionAnteriorId={} ubicacionNuevaId={}",
                    saved.getId(),
                    objeto.getId(),
                    estadoAnterior,
                    dto.estado(),
                    ubicacionAnterior.getId(),
                    nuevaUbicacion.getId()
            );
        } else {
            log.info("event=inventario.updated inventarioId={} objetoMuseoId={} estado={}", saved.getId(), objeto.getId(), saved.getEstado());
        }
        return InventarioMapper.toResponse(saved);
    }

    @Transactional
    public void bajaLogica(Long id) {
        Inventario entity = buscarActivo(id);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        inventarioRepository.save(entity);
        log.info("event=inventario.deleted inventarioId={} objetoMuseoId={}", entity.getId(), entity.getObjetoMuseo().getId());
    }

    private void registrarMovimiento(Inventario inventario, TipoMovimientoInventario tipo, Ubicacion origen, Ubicacion destino) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setObjetoMuseo(inventario.getObjetoMuseo());
        movimiento.setTipo(tipo);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setUbicacionOrigen(origen);
        movimiento.setUbicacionDestino(destino);
        movimiento.setObservaciones("Movimiento generado desde inventario");
        movimientoInventarioRepository.save(movimiento);
        log.info(
                "event=movimiento_inventario.created movimientoInventarioId={} objetoMuseoId={} tipo={} ubicacionOrigenId={} ubicacionDestinoId={}",
                movimiento.getId(),
                inventario.getObjetoMuseo().getId(),
                tipo,
                origen != null ? origen.getId() : null,
                destino != null ? destino.getId() : null
        );
    }

    private TipoMovimientoInventario resolverTipoMovimiento(EstadoInventario anterior, EstadoInventario nuevo, boolean cambioUbicacion) {
        if (nuevo == EstadoInventario.EN_EXHIBICION) {
            return TipoMovimientoInventario.SALIDA_EXHIBICION;
        }
        if (anterior == EstadoInventario.EN_EXHIBICION && nuevo == EstadoInventario.DISPONIBLE) {
            return TipoMovimientoInventario.DEVOLUCION_EXHIBICION;
        }
        if (cambioUbicacion) {
            return TipoMovimientoInventario.CAMBIO_UBICACION;
        }
        return TipoMovimientoInventario.RESTAURACION;
    }

    private Inventario buscarActivo(Long id) {
        Inventario entity = inventarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Inventario no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Inventario no encontrado");
        }
        return entity;
    }

    private ObjetoMuseo buscarObjeto(Long id) {
        ObjetoMuseo entity = objetoMuseoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Objeto de museo no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Objeto de museo no encontrado");
        }
        return entity;
    }

    private Ubicacion buscarUbicacion(Long id) {
        Ubicacion entity = ubicacionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ubicacion no encontrada"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Ubicacion no encontrada");
        }
        return entity;
    }
}
