package com.proveedores.service;

import com.proveedores.dto.ExhibicionObjetoRequestDTO;
import com.proveedores.dto.ExhibicionObjetoResponseDTO;
import com.proveedores.entity.EstadoExhibicion;
import com.proveedores.entity.EstadoExhibicionObjeto;
import com.proveedores.entity.Exhibicion;
import com.proveedores.entity.ExhibicionObjeto;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.Usuario;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.ExhibicionObjetoMapper;
import com.proveedores.repository.ExhibicionObjetoRepository;
import com.proveedores.repository.ExhibicionRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExhibicionObjetoService {

    private static final Logger log = LoggerFactory.getLogger(ExhibicionObjetoService.class);

    private final ExhibicionObjetoRepository exhibicionObjetoRepository;
    private final ExhibicionRepository exhibicionRepository;
    private final ObjetoMuseoRepository objetoMuseoRepository;
    private final UsuarioRepository usuarioRepository;

    public ExhibicionObjetoService(ExhibicionObjetoRepository exhibicionObjetoRepository, ExhibicionRepository exhibicionRepository, ObjetoMuseoRepository objetoMuseoRepository, UsuarioRepository usuarioRepository) {
        this.exhibicionObjetoRepository = exhibicionObjetoRepository;
        this.exhibicionRepository = exhibicionRepository;
        this.objetoMuseoRepository = objetoMuseoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public ExhibicionObjetoResponseDTO crear(ExhibicionObjetoRequestDTO dto) {
        Exhibicion exhibicion = buscarExhibicion(dto.exhibicionId());
        ObjetoMuseo objeto = buscarObjeto(dto.objetoMuseoId());
        validarObjetoNoEsteEnOtraExhibicionActiva(objeto.getId(), null, exhibicion);
        ExhibicionObjeto entity = ExhibicionObjetoMapper.toEntity(dto);
        entity.setExhibicion(exhibicion);
        entity.setObjetoMuseo(objeto);
        entity.setVerificadoPor(buscarUsuarioOpcional(dto.verificadoPorUsuarioId()));
        ExhibicionObjeto saved = exhibicionObjetoRepository.save(entity);
        log.info("event=exhibicion_objeto.created exhibicionObjetoId={} exhibicionId={} objetoMuseoId={} estado={}", saved.getId(), exhibicion.getId(), objeto.getId(), saved.getEstado());
        return ExhibicionObjetoMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ExhibicionObjetoResponseDTO obtenerPorId(Long id) {
        return ExhibicionObjetoMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<ExhibicionObjetoResponseDTO> listar() {
        return exhibicionObjetoRepository.findAll().stream().filter(e -> !e.getEliminado()).map(ExhibicionObjetoMapper::toResponse).toList();
    }

    @Transactional
    public ExhibicionObjetoResponseDTO actualizar(Long id, ExhibicionObjetoRequestDTO dto) {
        ExhibicionObjeto entity = buscarActivo(id);
        Exhibicion exhibicion = buscarExhibicion(dto.exhibicionId());
        ObjetoMuseo objeto = buscarObjeto(dto.objetoMuseoId());
        validarObjetoNoEsteEnOtraExhibicionActiva(objeto.getId(), id, exhibicion);
        entity.setExhibicion(exhibicion);
        entity.setObjetoMuseo(objeto);
        entity.setFechaInclusion(dto.fechaInclusion());
        entity.setFechaRetiro(dto.fechaRetiro());
        entity.setEstado(dto.estado());
        entity.setDevolucionVerificada(Boolean.TRUE.equals(dto.devolucionVerificada()));
        entity.setVerificadoPor(buscarUsuarioOpcional(dto.verificadoPorUsuarioId()));
        entity.setFechaVerificacion(dto.fechaVerificacion());
        entity.setObservacionesDevolucion(dto.observacionesDevolucion());
        ExhibicionObjeto saved = exhibicionObjetoRepository.save(entity);
        log.info("event=exhibicion_objeto.updated exhibicionObjetoId={} exhibicionId={} objetoMuseoId={} estado={}", saved.getId(), exhibicion.getId(), objeto.getId(), saved.getEstado());
        return ExhibicionObjetoMapper.toResponse(saved);
    }

    @Transactional
    public ExhibicionObjetoResponseDTO verificarDevolucion(Long id, Long usuarioId, String observaciones) {
        ExhibicionObjeto entity = buscarActivo(id);
        entity.setEstado(EstadoExhibicionObjeto.DEVUELTO);
        entity.setDevolucionVerificada(true);
        entity.setVerificadoPor(buscarUsuarioOpcional(usuarioId));
        entity.setFechaVerificacion(LocalDateTime.now());
        entity.setObservacionesDevolucion(observaciones);
        ExhibicionObjeto saved = exhibicionObjetoRepository.save(entity);
        log.info("event=exhibicion_objeto.return_verified exhibicionObjetoId={} exhibicionId={} objetoMuseoId={}", saved.getId(), saved.getExhibicion().getId(), saved.getObjetoMuseo().getId());
        return ExhibicionObjetoMapper.toResponse(saved);
    }

    @Transactional
    public void bajaLogica(Long id) {
        ExhibicionObjeto entity = buscarActivo(id);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        exhibicionObjetoRepository.save(entity);
        log.info("event=exhibicion_objeto.deleted exhibicionObjetoId={} exhibicionId={} objetoMuseoId={}", entity.getId(), entity.getExhibicion().getId(), entity.getObjetoMuseo().getId());
    }

    private void validarObjetoNoEsteEnOtraExhibicionActiva(Long objetoId, Long relacionActualId, Exhibicion exhibicion) {
        if (exhibicion.getEstado() != EstadoExhibicion.ACTIVA) {
            return;
        }
        boolean existeActiva = exhibicionObjetoRepository.findByObjetoMuseoIdAndEliminadoFalse(objetoId).stream()
                .filter(relacion -> relacionActualId == null || !relacion.getId().equals(relacionActualId))
                .anyMatch(relacion -> !relacion.getExhibicion().getEliminado() && relacion.getExhibicion().getEstado() == EstadoExhibicion.ACTIVA);
        if (existeActiva) {
            log.warn("event=exhibicion_objeto.business_error reason=objeto_en_exhibicion_activa objetoMuseoId={} exhibicionId={}", objetoId, exhibicion.getId());
            throw new BusinessException("El objeto ya esta asociado a una exhibicion activa");
        }
    }

    private ExhibicionObjeto buscarActivo(Long id) {
        ExhibicionObjeto entity = exhibicionObjetoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Objeto de exhibicion no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Objeto de exhibicion no encontrado");
        }
        return entity;
    }

    private Exhibicion buscarExhibicion(Long id) {
        Exhibicion entity = exhibicionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Exhibicion no encontrada"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Exhibicion no encontrada");
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
