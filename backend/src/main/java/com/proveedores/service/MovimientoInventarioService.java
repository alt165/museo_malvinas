package com.proveedores.service;

import com.proveedores.dto.MovimientoInventarioResponseDTO;
import com.proveedores.entity.MovimientoInventario;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.MovimientoInventarioMapper;
import com.proveedores.repository.MovimientoInventarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovimientoInventarioService {

    private final MovimientoInventarioRepository movimientoInventarioRepository;
    public MovimientoInventarioService(MovimientoInventarioRepository movimientoInventarioRepository) {
        this.movimientoInventarioRepository = movimientoInventarioRepository;
    }

    @Transactional(readOnly = true)
    public MovimientoInventarioResponseDTO obtenerPorId(Long id) {
        return MovimientoInventarioMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponseDTO> listar() {
        return movimientoInventarioRepository.findAll().stream().filter(e -> !e.getEliminado()).map(MovimientoInventarioMapper::toResponse).toList();
    }

    private MovimientoInventario buscarActivo(Long id) {
        MovimientoInventario entity = movimientoInventarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Movimiento de inventario no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Movimiento de inventario no encontrado");
        }
        return entity;
    }

}
