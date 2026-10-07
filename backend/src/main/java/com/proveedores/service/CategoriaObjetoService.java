package com.proveedores.service;

import com.proveedores.dto.CategoriaObjetoRequestDTO;
import com.proveedores.dto.CategoriaObjetoResponseDTO;
import com.proveedores.entity.CategoriaObjeto;
import com.proveedores.exception.ConflictException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.CategoriaObjetoMapper;
import com.proveedores.repository.CategoriaObjetoRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaObjetoService {

    private final CategoriaObjetoRepository categoriaObjetoRepository;

    public CategoriaObjetoService(CategoriaObjetoRepository categoriaObjetoRepository) {
        this.categoriaObjetoRepository = categoriaObjetoRepository;
    }

    @Transactional
    public CategoriaObjetoResponseDTO crear(CategoriaObjetoRequestDTO dto) {
        CategoriaObjeto categoria = categoriaObjetoRepository.findByNombre(dto.nombre()).orElse(null);
        if (categoria != null && !categoria.getEliminado()) {
            throw new ConflictException("Ya existe una categoria activa con ese nombre");
        }
        if (categoria == null) {
            categoria = CategoriaObjetoMapper.toEntity(dto);
        } else {
            categoria.setActivo(true);
            categoria.setEliminado(false);
            categoria.setFechaEliminacion(null);
            categoria.setDescripcion(dto.descripcion());
        }
        return CategoriaObjetoMapper.toResponse(categoriaObjetoRepository.save(categoria));
    }

    @Transactional(readOnly = true)
    public CategoriaObjetoResponseDTO obtenerPorId(Long id) {
        return CategoriaObjetoMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<CategoriaObjetoResponseDTO> listar() {
        return categoriaObjetoRepository.findAll().stream()
                .filter(e -> !e.getEliminado())
                .map(CategoriaObjetoMapper::toResponse)
                .sorted(Comparator.comparing(CategoriaObjetoResponseDTO::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public CategoriaObjetoResponseDTO actualizar(Long id, CategoriaObjetoRequestDTO dto) {
        CategoriaObjeto entity = buscarActivo(id);
        entity.setNombre(dto.nombre());
        entity.setDescripcion(dto.descripcion());
        return CategoriaObjetoMapper.toResponse(categoriaObjetoRepository.save(entity));
    }

    @Transactional
    public void bajaLogica(Long id) {
        CategoriaObjeto entity = buscarActivo(id);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(com.proveedores.time.MuseoTime.now());
        categoriaObjetoRepository.save(entity);
    }

    private CategoriaObjeto buscarActivo(Long id) {
        CategoriaObjeto entity = categoriaObjetoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Categoria no encontrada");
        }
        return entity;
    }
}
