package com.proveedores.service;

import com.proveedores.dto.AristaGrafoObjetoDTO;
import com.proveedores.dto.NodoGrafoObjetoDTO;
import com.proveedores.dto.ObjetoGrafoResponseDTO;
import com.proveedores.dto.RelacionObjetoRequestDTO;
import com.proveedores.dto.RelacionObjetoPorObjetoResponseDTO;
import com.proveedores.dto.RelacionObjetoResponseDTO;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.RelacionObjeto;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.RelacionObjetoMapper;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.repository.RelacionObjetoRepository;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelacionObjetoService {

    private static final int PROFUNDIDAD_GRAFO_DEFAULT = 1;
    private static final int PROFUNDIDAD_GRAFO_MAXIMA = 3;

    private final RelacionObjetoRepository relacionObjetoRepository;
    private final ObjetoMuseoRepository objetoMuseoRepository;

    public RelacionObjetoService(RelacionObjetoRepository relacionObjetoRepository, ObjetoMuseoRepository objetoMuseoRepository) {
        this.relacionObjetoRepository = relacionObjetoRepository;
        this.objetoMuseoRepository = objetoMuseoRepository;
    }

    @Transactional
    public RelacionObjetoResponseDTO crear(RelacionObjetoRequestDTO dto) {
        return crear(dto, null);
    }

    @Transactional
    public RelacionObjetoResponseDTO crear(RelacionObjetoRequestDTO dto, String creadoPor) {
        validarRelacion(dto, null);
        RelacionObjeto entity = RelacionObjetoMapper.toEntity(dto);
        entity.setObjetoOrigen(buscarObjeto(dto.objetoOrigenId()));
        entity.setObjetoDestino(buscarObjeto(dto.objetoDestinoId()));
        entity.setTipoRelacion(normalizarTipo(dto.tipoRelacion()));
        entity.setDescripcion(normalizarDescripcion(dto.descripcion()));
        entity.setFechaCreacion(LocalDateTime.now());
        entity.setCreadoPor(creadoPor);
        return RelacionObjetoMapper.toResponse(relacionObjetoRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public RelacionObjetoResponseDTO obtenerPorId(Long id) {
        return RelacionObjetoMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<RelacionObjetoResponseDTO> listar() {
        return relacionObjetoRepository.findAll().stream().filter(e -> !e.getEliminado()).map(RelacionObjetoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RelacionObjetoPorObjetoResponseDTO> listarPorObjeto(Long objetoId) {
        buscarObjeto(objetoId);
        return relacionObjetoRepository.findAllByObjetoMuseoId(objetoId).stream()
                .map(relacion -> toPorObjetoResponse(relacion, objetoId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ObjetoGrafoResponseDTO obtenerGrafoRelaciones(Long objetoId, Integer profundidad) {
        int profundidadNormalizada = normalizarProfundidad(profundidad);
        ObjetoMuseo objetoInicial = buscarObjeto(objetoId);
        Map<Long, NodoGrafoObjetoDTO> nodes = new LinkedHashMap<>();
        Map<Long, AristaGrafoObjetoDTO> edges = new LinkedHashMap<>();
        Map<Long, Integer> distancias = new HashMap<>();
        Queue<Long> pendientes = new ArrayDeque<>();
        Set<Long> procesados = new HashSet<>();

        nodes.put(objetoInicial.getId(), toNodo(objetoInicial));
        distancias.put(objetoInicial.getId(), 0);
        pendientes.add(objetoInicial.getId());

        while (!pendientes.isEmpty()) {
            Long actualId = pendientes.poll();
            int distancia = distancias.getOrDefault(actualId, 0);
            if (!procesados.add(actualId) || distancia >= profundidadNormalizada) {
                continue;
            }

            for (RelacionObjeto relacion : relacionObjetoRepository.findAllByObjetoMuseoId(actualId)) {
                if (!relacionActivaConObjetosActivos(relacion)) {
                    continue;
                }

                ObjetoMuseo origen = relacion.getObjetoOrigen();
                ObjetoMuseo destino = relacion.getObjetoDestino();
                nodes.putIfAbsent(origen.getId(), toNodo(origen));
                nodes.putIfAbsent(destino.getId(), toNodo(destino));
                edges.putIfAbsent(relacion.getId(), toArista(relacion));

                Long vecinoId = origen.getId().equals(actualId) ? destino.getId() : origen.getId();
                if (!distancias.containsKey(vecinoId)) {
                    distancias.put(vecinoId, distancia + 1);
                    pendientes.add(vecinoId);
                }
            }
        }

        return new ObjetoGrafoResponseDTO(new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()));
    }

    @Transactional
    public RelacionObjetoResponseDTO actualizar(Long id, RelacionObjetoRequestDTO dto) {
        RelacionObjeto entity = buscarActivo(id);
        validarRelacion(dto, id);
        entity.setObjetoOrigen(buscarObjeto(dto.objetoOrigenId()));
        entity.setObjetoDestino(buscarObjeto(dto.objetoDestinoId()));
        entity.setTipoRelacion(normalizarTipo(dto.tipoRelacion()));
        entity.setDescripcion(normalizarDescripcion(dto.descripcion()));
        return RelacionObjetoMapper.toResponse(relacionObjetoRepository.save(entity));
    }

    @Transactional
    public void bajaLogica(Long id) {
        RelacionObjeto entity = buscarActivo(id);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        relacionObjetoRepository.save(entity);
    }

    private RelacionObjeto buscarActivo(Long id) {
        RelacionObjeto entity = relacionObjetoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Relacion entre objetos no encontrada"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Relacion entre objetos no encontrada");
        }
        return entity;
    }

    private ObjetoMuseo buscarObjeto(Long id) {
        ObjetoMuseo entity = objetoMuseoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Objeto de museo no encontrado"));
        if (Boolean.TRUE.equals(entity.getEliminado()) || Boolean.FALSE.equals(entity.getActivo())) {
            throw new ResourceNotFoundException("Objeto de museo no encontrado");
        }
        return entity;
    }

    private void validarRelacion(RelacionObjetoRequestDTO dto, Long relacionActualId) {
        if (dto.objetoOrigenId().equals(dto.objetoDestinoId())) {
            throw new BusinessException("El objeto origen y destino no pueden ser el mismo");
        }
        String tipoRelacion = normalizarTipo(dto.tipoRelacion());
        relacionObjetoRepository.findByObjetoOrigenIdAndObjetoDestinoIdAndTipoRelacionAndEliminadoFalse(
                        dto.objetoOrigenId(),
                        dto.objetoDestinoId(),
                        tipoRelacion
                )
                .filter(relacion -> relacionActualId == null || !relacion.getId().equals(relacionActualId))
                .ifPresent(relacion -> {
                    throw new BusinessException("Ya existe una relacion igual entre los objetos");
                });
    }

    private String normalizarTipo(String tipoRelacion) {
        return tipoRelacion == null ? null : tipoRelacion.trim();
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        return descripcion.trim();
    }

    private int normalizarProfundidad(Integer profundidad) {
        int value = profundidad == null ? PROFUNDIDAD_GRAFO_DEFAULT : profundidad;
        if (value < 1) {
            throw new BusinessException("La profundidad debe ser mayor o igual a 1");
        }
        if (value > PROFUNDIDAD_GRAFO_MAXIMA) {
            throw new BusinessException("La profundidad maxima permitida es 3");
        }
        return value;
    }

    private boolean relacionActivaConObjetosActivos(RelacionObjeto relacion) {
        return !Boolean.TRUE.equals(relacion.getEliminado())
                && relacion.getObjetoOrigen() != null
                && relacion.getObjetoDestino() != null
                && !Boolean.TRUE.equals(relacion.getObjetoOrigen().getEliminado())
                && !Boolean.TRUE.equals(relacion.getObjetoDestino().getEliminado())
                && !Boolean.FALSE.equals(relacion.getObjetoOrigen().getActivo())
                && !Boolean.FALSE.equals(relacion.getObjetoDestino().getActivo());
    }

    private NodoGrafoObjetoDTO toNodo(ObjetoMuseo objeto) {
        return new NodoGrafoObjetoDTO(objeto.getId(), objeto.getDenominacionObjeto(), objeto.getNumeroInventario());
    }

    private AristaGrafoObjetoDTO toArista(RelacionObjeto relacion) {
        return new AristaGrafoObjetoDTO(
                relacion.getId(),
                relacion.getObjetoOrigen().getId(),
                relacion.getObjetoDestino().getId(),
                relacion.getTipoRelacion(),
                relacion.getDescripcion()
        );
    }

    private RelacionObjetoPorObjetoResponseDTO toPorObjetoResponse(RelacionObjeto relacion, Long objetoId) {
        String direccion = relacion.getObjetoOrigen().getId().equals(objetoId) ? "SALIENTE" : "ENTRANTE";
        return new RelacionObjetoPorObjetoResponseDTO(
                relacion.getId(),
                relacion.getObjetoOrigen().getId(),
                relacion.getObjetoOrigen().getNumeroInventario(),
                relacion.getObjetoOrigen().getDenominacionObjeto(),
                relacion.getObjetoDestino().getId(),
                relacion.getObjetoDestino().getNumeroInventario(),
                relacion.getObjetoDestino().getDenominacionObjeto(),
                relacion.getTipoRelacion(),
                relacion.getDescripcion(),
                direccion
        );
    }
}
