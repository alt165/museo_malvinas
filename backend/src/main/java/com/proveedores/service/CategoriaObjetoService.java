package com.proveedores.service;

import com.proveedores.dto.CategoriaObjetoRequestDTO;
import com.proveedores.dto.CategoriaObjetoResponseDTO;
import com.proveedores.entity.CategoriaObjeto;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.CategoriaObjetoMapper;
import com.proveedores.repository.CategoriaObjetoRepository;
import java.time.LocalDateTime;
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
        return CategoriaObjetoMapper.toResponse(categoriaObjetoRepository.save(CategoriaObjetoMapper.toEntity(dto)));
    }

    @Transactional(readOnly = true)
    public CategoriaObjetoResponseDTO obtenerPorId(Long id) {
        return CategoriaObjetoMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<CategoriaObjetoResponseDTO> listar() {
        return categoriaObjetoRepository.findAll().stream().filter(e -> !e.getEliminado()).map(CategoriaObjetoMapper::toResponse).toList();
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
        entity.setFechaEliminacion(LocalDateTime.now());
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
