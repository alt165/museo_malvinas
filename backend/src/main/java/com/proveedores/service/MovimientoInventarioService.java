package com.proveedores.service;

import com.proveedores.dto.MovimientoInventarioRequestDTO;
import com.proveedores.dto.MovimientoInventarioResponseDTO;
import com.proveedores.entity.MovimientoInventario;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.Ubicacion;
import com.proveedores.entity.Usuario;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.MovimientoInventarioMapper;
import com.proveedores.repository.MovimientoInventarioRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.repository.UbicacionRepository;
import com.proveedores.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovimientoInventarioService {

    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final ObjetoMuseoRepository objetoMuseoRepository;
    private final UbicacionRepository ubicacionRepository;
    private final UsuarioRepository usuarioRepository;

    public MovimientoInventarioService(MovimientoInventarioRepository movimientoInventarioRepository, ObjetoMuseoRepository objetoMuseoRepository, UbicacionRepository ubicacionRepository, UsuarioRepository usuarioRepository) {
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.objetoMuseoRepository = objetoMuseoRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public MovimientoInventarioResponseDTO crear(MovimientoInventarioRequestDTO dto) {
        MovimientoInventario entity = MovimientoInventarioMapper.toEntity(dto);
        entity.setObjetoMuseo(buscarObjeto(dto.objetoMuseoId()));
        entity.setUbicacionOrigen(buscarUbicacionOpcional(dto.ubicacionOrigenId()));
        entity.setUbicacionDestino(buscarUbicacionOpcional(dto.ubicacionDestinoId()));
        entity.setUsuario(buscarUsuarioOpcional(dto.usuarioId()));
        return MovimientoInventarioMapper.toResponse(movimientoInventarioRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public MovimientoInventarioResponseDTO obtenerPorId(Long id) {
        return MovimientoInventarioMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponseDTO> listar() {
        return movimientoInventarioRepository.findAll().stream().filter(e -> !e.getEliminado()).map(MovimientoInventarioMapper::toResponse).toList();
    }

    @Transactional
    public MovimientoInventarioResponseDTO actualizar(Long id, MovimientoInventarioRequestDTO dto) {
        MovimientoInventario entity = buscarActivo(id);
        entity.setObjetoMuseo(buscarObjeto(dto.objetoMuseoId()));
        entity.setTipo(dto.tipo());
        entity.setFecha(dto.fecha());
        entity.setUbicacionOrigen(buscarUbicacionOpcional(dto.ubicacionOrigenId()));
        entity.setUbicacionDestino(buscarUbicacionOpcional(dto.ubicacionDestinoId()));
        entity.setUsuario(buscarUsuarioOpcional(dto.usuarioId()));
        entity.setObservaciones(dto.observaciones());
        return MovimientoInventarioMapper.toResponse(movimientoInventarioRepository.save(entity));
    }

    @Transactional
    public void bajaLogica(Long id) {
        MovimientoInventario entity = buscarActivo(id);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        movimientoInventarioRepository.save(entity);
    }

    private MovimientoInventario buscarActivo(Long id) {
        MovimientoInventario entity = movimientoInventarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Movimiento de inventario no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Movimiento de inventario no encontrado");
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

    private Ubicacion buscarUbicacionOpcional(Long id) {
        if (id == null) {
            return null;
        }
        Ubicacion entity = ubicacionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ubicacion no encontrada"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Ubicacion no encontrada");
        }
        return entity;
    }

    private Usuario buscarUsuarioOpcional(Long id) {
        if (id == null) {
            return null;
        }
        Usuario entity = usuarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }
        return entity;
    }
}
