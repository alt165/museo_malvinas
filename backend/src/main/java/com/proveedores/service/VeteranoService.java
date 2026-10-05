package com.proveedores.service;

import com.proveedores.dto.VeteranoConsultaResponseDTO;
import com.proveedores.dto.VeteranoRequestDTO;
import com.proveedores.dto.VeteranoResponseDTO;
import com.proveedores.entity.Fuerza;
import com.proveedores.entity.Veterano;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.VeteranoMapper;
import com.proveedores.repository.ObjetoVeteranoRepository;
import com.proveedores.repository.VeteranoRepository;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VeteranoService {

    private final VeteranoRepository veteranoRepository;
    private final ObjetoVeteranoRepository objetoVeteranoRepository;

    public VeteranoService(VeteranoRepository veteranoRepository, ObjetoVeteranoRepository objetoVeteranoRepository) {
        this.veteranoRepository = veteranoRepository;
        this.objetoVeteranoRepository = objetoVeteranoRepository;
    }

    @Transactional
    public VeteranoResponseDTO crear(VeteranoRequestDTO dto) {
        return VeteranoMapper.toResponse(veteranoRepository.save(VeteranoMapper.toEntity(dto)));
    }

    @Transactional(readOnly = true)
    public VeteranoResponseDTO obtenerPorId(Long id) {
        return VeteranoMapper.toResponse(buscarActivo(id));
    }

    @Transactional(readOnly = true)
    public List<VeteranoResponseDTO> listar() {
        return veteranoRepository.findAll().stream().filter(e -> !e.getEliminado()).map(VeteranoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<VeteranoConsultaResponseDTO> buscar(String texto, Pageable pageable) {
        String filtro = texto == null || texto.isBlank() ? null : texto.trim();
        int size = Math.min(Math.max(pageable.getPageSize(), 1), 50);
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), size,
                Sort.by(Sort.Direction.ASC, "nombre")
                        .and(Sort.by(Sort.Direction.ASC, "apellido"))
                        .and(Sort.by(Sort.Direction.ASC, "id")));
        Specification<Veterano> specification = (root, query, criteriaBuilder) -> {
            var noEliminado = criteriaBuilder.isFalse(root.get("eliminado"));
            if (filtro == null) {
                return noEliminado;
            }

            String patron = "%" + escaparPatronLike(filtro.toLowerCase(Locale.ROOT)) + "%";
            var patronNormalizado = criteriaBuilder.function("unaccent", String.class,
                    criteriaBuilder.lower(criteriaBuilder.literal(patron)));
            var nombre = criteriaBuilder.like(criteriaBuilder.function("unaccent", String.class,
                    criteriaBuilder.lower(root.get("nombre"))), patronNormalizado, '\\');
            var apellido = criteriaBuilder.like(criteriaBuilder.function("unaccent", String.class,
                    criteriaBuilder.lower(root.get("apellido"))), patronNormalizado, '\\');
            var fuerzaEnum = criteriaBuilder.like(criteriaBuilder.function("unaccent", String.class,
                    criteriaBuilder.lower(root.get("fuerza").as(String.class))), patronNormalizado, '\\');
            jakarta.persistence.criteria.CriteriaBuilder.Case<String> etiquetaFuerza = criteriaBuilder.selectCase();
            etiquetaFuerza.when(criteriaBuilder.equal(root.get("fuerza"), Fuerza.EJERCITO), "Ejercito")
                    .when(criteriaBuilder.equal(root.get("fuerza"), Fuerza.ARMADA), "Armada")
                    .when(criteriaBuilder.equal(root.get("fuerza"), Fuerza.FUERZA_AEREA), "Fuerza Aerea")
                    .when(criteriaBuilder.equal(root.get("fuerza"), Fuerza.PREFECTURA), "Prefectura")
                    .when(criteriaBuilder.equal(root.get("fuerza"), Fuerza.GENDARMERIA), "Gendarmeria")
                    .otherwise("Civil");
            var fuerzaVisible = criteriaBuilder.like(criteriaBuilder.function("unaccent", String.class,
                    criteriaBuilder.lower(etiquetaFuerza)), patronNormalizado, '\\');
            return criteriaBuilder.and(noEliminado,
                    criteriaBuilder.or(nombre, apellido, fuerzaEnum, fuerzaVisible));
        };

        Page<Veterano> pagina = veteranoRepository.findAll(specification, pageRequest);
        List<Long> ids = pagina.getContent().stream().map(Veterano::getId).toList();
        Map<Long, Long> conteos = ids.isEmpty() ? Map.of() : objetoVeteranoRepository.contarPorVeteranoIds(ids).stream()
                .collect(Collectors.toMap(fila -> (Long) fila[0], fila -> (Long) fila[1]));
        return pagina.map(veterano -> {
            VeteranoResponseDTO dto = VeteranoMapper.toResponse(veterano);
            return new VeteranoConsultaResponseDTO(dto.id(), dto.nombre(), dto.apellido(), dto.nombreCompleto(),
                    dto.fuerza(), dto.fechaNacimiento(), dto.fechaFallecimiento(), dto.historia(),
                    conteos.getOrDefault(veterano.getId(), 0L));
        });
    }

    private String escaparPatronLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Transactional
    public VeteranoResponseDTO actualizar(Long id, VeteranoRequestDTO dto) {
        Veterano entity = buscarActivo(id);
        entity.setNombre(dto.nombre());
        entity.setApellido(dto.apellido());
        entity.setFuerza(dto.fuerza());
        entity.setFechaNacimiento(dto.fechaNacimiento());
        entity.setFechaFallecimiento(dto.fechaFallecimiento());
        entity.setHistoria(dto.historia());
        return VeteranoMapper.toResponse(veteranoRepository.save(entity));
    }

    @Transactional
    public void bajaLogica(Long id) {
        Veterano entity = buscarActivo(id);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(com.proveedores.time.MuseoTime.now());
        veteranoRepository.save(entity);
    }

    public Veterano buscarActivo(Long id) {
        Veterano entity = veteranoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Veterano no encontrado"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Veterano no encontrado");
        }
        return entity;
    }
}
