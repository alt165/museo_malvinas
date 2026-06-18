package com.proveedores.service;

import com.proveedores.dto.ObjetoVeteranoRequestDTO;
import com.proveedores.dto.ObjetoVeteranoResponseDTO;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.ObjetoVeterano;
import com.proveedores.entity.Veterano;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.ObjetoVeteranoMapper;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.repository.ObjetoVeteranoRepository;
import com.proveedores.repository.VeteranoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObjetoVeteranoService {

    private final ObjetoVeteranoRepository objetoVeteranoRepository;
    private final ObjetoMuseoRepository objetoMuseoRepository;
    private final VeteranoRepository veteranoRepository;

    public ObjetoVeteranoService(
            ObjetoVeteranoRepository objetoVeteranoRepository,
            ObjetoMuseoRepository objetoMuseoRepository,
            VeteranoRepository veteranoRepository
    ) {
        this.objetoVeteranoRepository = objetoVeteranoRepository;
        this.objetoMuseoRepository = objetoMuseoRepository;
        this.veteranoRepository = veteranoRepository;
    }

    @Transactional
    public ObjetoVeteranoResponseDTO crear(ObjetoVeteranoRequestDTO dto) {
        validarRelacionDisponible(dto);
        ObjetoMuseo objeto = buscarObjeto(dto.objetoMuseoId());
        Veterano veterano = buscarVeterano(dto.veteranoId());
        ObjetoVeterano entity = ObjetoVeteranoMapper.toEntity(dto);
        entity.setObjetoMuseo(objeto);
        entity.setVeterano(veterano);
        return ObjetoVeteranoMapper.toResponse(objetoVeteranoRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public ObjetoVeteranoResponseDTO obtenerPorId(Long id) {
        return ObjetoVeteranoMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<ObjetoVeteranoResponseDTO> listar() {
        return objetoVeteranoRepository.findAll().stream()
                .filter(relacion -> !relacion.getEliminado())
                .map(ObjetoVeteranoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ObjetoVeteranoResponseDTO> listarPorVeterano(Long veteranoId) {
        buscarVeterano(veteranoId);
        return objetoVeteranoRepository.findByVeteranoIdAndEliminadoFalse(veteranoId).stream()
                .map(ObjetoVeteranoMapper::toResponse)
                .toList();
    }

    @Transactional
    public void bajaLogica(Long id) {
        ObjetoVeterano entity = buscarActivo(id);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(LocalDateTime.now());
        objetoVeteranoRepository.save(entity);
    }

    private void validarRelacionDisponible(ObjetoVeteranoRequestDTO dto) {
        boolean existe = objetoVeteranoRepository.existsByObjetoMuseoIdAndVeteranoIdAndTipoRelacionAndEliminadoFalse(
                dto.objetoMuseoId(),
                dto.veteranoId(),
                dto.tipoRelacion()
        );
        if (existe) {
            throw new BusinessException("Ya existe una relacion activa entre el objeto y el veterano con ese tipo");
        }
    }

    private ObjetoVeterano buscarActivo(Long id) {
        ObjetoVeterano entity = objetoVeteranoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Relacion objeto-veterano no encontrada"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Relacion objeto-veterano no encontrada");
        }
        return entity;
    }

    private ObjetoMuseo buscarObjeto(Long id) {
        ObjetoMuseo entity = objetoMuseoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Objeto de museo no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Objeto de museo no encontrado");
        }
        return entity;
    }

    private Veterano buscarVeterano(Long id) {
        Veterano entity = veteranoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veterano no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Veterano no encontrado");
        }
        return entity;
    }
}
