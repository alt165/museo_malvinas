package com.proveedores.service;

import com.proveedores.dto.AristaGrafoObjetoDTO;
import com.proveedores.dto.NodoGrafoObjetoDTO;
import com.proveedores.dto.ObjetoGrafoResponseDTO;
import com.proveedores.dto.RelacionElementoResponseDTO;
import com.proveedores.dto.RelacionObjetoRequestDTO;
import com.proveedores.dto.RelacionObjetoResponseDTO;
import com.proveedores.dto.TipoNodoRelacion;
import com.proveedores.dto.TipoVinculoRelacion;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.ObjetoVeterano;
import com.proveedores.entity.RelacionObjeto;
import com.proveedores.entity.Veterano;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ConflictException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.mapper.RelacionObjetoMapper;
import com.proveedores.repository.EmbargoObjetoRepository;
import com.proveedores.repository.ObjetoMuseoRepository;
import com.proveedores.repository.ObjetoVeteranoRepository;
import com.proveedores.repository.RelacionObjetoRepository;
import com.proveedores.repository.VeteranoRepository;
import com.proveedores.security.ObjetoVisibilityPolicy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelacionObjetoService {

    private static final int PROFUNDIDAD_GRAFO_DEFAULT = 1;
    private static final int PROFUNDIDAD_GRAFO_MAXIMA = 3;
    private static final String OBJETO_PREFIX = "OBJETO-";
    private static final String PERSONA_PREFIX = "PERSONA-";

    private final RelacionObjetoRepository relacionObjetoRepository;
    private final ObjetoMuseoRepository objetoMuseoRepository;
    private final ObjetoVeteranoRepository objetoVeteranoRepository;
    private final VeteranoRepository veteranoRepository;
    private final EmbargoObjetoRepository embargoObjetoRepository;

    public RelacionObjetoService(
            RelacionObjetoRepository relacionObjetoRepository,
            ObjetoMuseoRepository objetoMuseoRepository,
            ObjetoVeteranoRepository objetoVeteranoRepository,
            VeteranoRepository veteranoRepository,
            EmbargoObjetoRepository embargoObjetoRepository
    ) {
        this.relacionObjetoRepository = relacionObjetoRepository;
        this.objetoMuseoRepository = objetoMuseoRepository;
        this.objetoVeteranoRepository = objetoVeteranoRepository;
        this.veteranoRepository = veteranoRepository;
        this.embargoObjetoRepository = embargoObjetoRepository;
    }

    @Transactional
    public RelacionObjetoResponseDTO crear(RelacionObjetoRequestDTO dto) {
        return crear(dto, null);
    }

    @Transactional
    public RelacionObjetoResponseDTO crear(RelacionObjetoRequestDTO dto, String creadoPor) {
        validarRelacion(dto, null);
        RelacionObjeto entity = RelacionObjetoMapper.toEntity(dto);
        entity.setObjetoOrigen(buscarObjetoActivo(dto.objetoOrigenId()));
        entity.setObjetoDestino(buscarObjetoActivo(dto.objetoDestinoId()));
        entity.setTipoRelacion(normalizarTipo(dto.tipoRelacion()));
        entity.setDescripcion(normalizarDescripcion(dto.descripcion()));
        entity.setFechaCreacion(com.proveedores.time.MuseoTime.now());
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
    public List<RelacionElementoResponseDTO> listarPorObjeto(Long objetoId) {
        ObjetoMuseo objeto = buscarObjetoVisible(objetoId);
        List<RelacionElementoResponseDTO> resultado = new ArrayList<>();
        relacionObjetoRepository.findAllByObjetoMuseoId(objetoId).stream()
                .filter(this::relacionActivaConObjetosVisibles)
                .map(relacion -> toElementoObjeto(relacion, objeto.getId()))
                .forEach(resultado::add);
        objetoVeteranoRepository.findByObjetoMuseoIdAndEliminadoFalse(objetoId).stream()
                .filter(this::relacionPersonaActiva)
                .map(this::toElementoPersona)
                .forEach(resultado::add);
        return ordenarElementos(resultado);
    }

    @Transactional(readOnly = true)
    public List<RelacionElementoResponseDTO> listarPorPersona(Long personaId) {
        buscarPersonaActiva(personaId);
        return ordenarElementos(objetoVeteranoRepository.findByVeteranoIdAndEliminadoFalse(personaId).stream()
                .filter(this::relacionPersonaActiva)
                .filter(relacion -> objetoVisible(relacion.getObjetoMuseo()))
                .map(this::toElementoObjetoDePersona)
                .toList());
    }

    @Transactional(readOnly = true)
    public ObjetoGrafoResponseDTO obtenerGrafoRelaciones(Long objetoId, Integer profundidad) {
        ObjetoMuseo objetoInicial = buscarObjetoVisible(objetoId);
        GrafoBuilder grafo = new GrafoBuilder();
        grafo.agregarNodo(toNodo(objetoInicial));
        expandirObjetos(grafo, Set.of(objetoInicial.getId()), normalizarProfundidad(profundidad));
        return grafo.build();
    }

    @Transactional(readOnly = true)
    public ObjetoGrafoResponseDTO obtenerGrafoRelacionesPersona(Long personaId, Integer profundidad) {
        Veterano persona = buscarPersonaActiva(personaId);
        GrafoBuilder grafo = new GrafoBuilder();
        grafo.agregarNodo(toNodo(persona));
        Set<Long> objetosDirectos = new LinkedHashSet<>();
        for (ObjetoVeterano relacion : objetoVeteranoRepository.findByVeteranoIdAndEliminadoFalse(personaId)) {
            if (!relacionPersonaActiva(relacion) || !objetoVisible(relacion.getObjetoMuseo())) continue;
            grafo.agregarNodo(toNodo(relacion.getObjetoMuseo()));
            grafo.agregarArista(toArista(relacion));
            objetosDirectos.add(relacion.getObjetoMuseo().getId());
        }
        expandirObjetos(grafo, objetosDirectos, normalizarProfundidad(profundidad));
        return grafo.build();
    }

    private void expandirObjetos(GrafoBuilder grafo, Set<Long> objetosIniciales, int profundidad) {
        Set<Long> visitados = new LinkedHashSet<>();
        Set<Long> frontera = new LinkedHashSet<>(objetosIniciales);
        for (int nivel = 0; nivel < profundidad && !frontera.isEmpty(); nivel++) {
            frontera.removeAll(visitados);
            if (frontera.isEmpty()) break;
            visitados.addAll(frontera);
            Set<Long> siguiente = new LinkedHashSet<>();

            for (RelacionObjeto relacion : relacionObjetoRepository.findAllByObjetoMuseoIds(frontera)) {
                if (!relacionActivaConObjetosVisibles(relacion)) continue;
                grafo.agregarNodo(toNodo(relacion.getObjetoOrigen()));
                grafo.agregarNodo(toNodo(relacion.getObjetoDestino()));
                grafo.agregarArista(toArista(relacion));
                siguiente.add(relacion.getObjetoOrigen().getId());
                siguiente.add(relacion.getObjetoDestino().getId());
            }

            Set<Long> personas = new LinkedHashSet<>();
            for (ObjetoVeterano relacion : objetoVeteranoRepository.findAllByObjetoMuseoIds(frontera)) {
                if (!relacionPersonaActiva(relacion) || !objetoVisible(relacion.getObjetoMuseo())) continue;
                grafo.agregarNodo(toNodo(relacion.getObjetoMuseo()));
                grafo.agregarNodo(toNodo(relacion.getVeterano()));
                grafo.agregarArista(toArista(relacion));
                personas.add(relacion.getVeterano().getId());
            }
            if (!personas.isEmpty()) {
                for (ObjetoVeterano relacion : objetoVeteranoRepository.findAllByVeteranoIds(personas)) {
                    if (!relacionPersonaActiva(relacion) || !objetoVisible(relacion.getObjetoMuseo())) continue;
                    grafo.agregarNodo(toNodo(relacion.getObjetoMuseo()));
                    grafo.agregarNodo(toNodo(relacion.getVeterano()));
                    grafo.agregarArista(toArista(relacion));
                    siguiente.add(relacion.getObjetoMuseo().getId());
                }
            }
            siguiente.removeAll(visitados);
            frontera = siguiente;
        }
    }

    @Transactional
    public RelacionObjetoResponseDTO actualizar(Long id, RelacionObjetoRequestDTO dto) {
        RelacionObjeto entity = buscarActivo(id);
        validarRelacion(dto, id);
        entity.setObjetoOrigen(buscarObjetoActivo(dto.objetoOrigenId()));
        entity.setObjetoDestino(buscarObjetoActivo(dto.objetoDestinoId()));
        entity.setTipoRelacion(normalizarTipo(dto.tipoRelacion()));
        entity.setDescripcion(normalizarDescripcion(dto.descripcion()));
        return RelacionObjetoMapper.toResponse(relacionObjetoRepository.save(entity));
    }

    @Transactional
    public void bajaLogica(Long id) {
        RelacionObjeto entity = buscarActivo(id);
        entity.setActivo(false);
        entity.setEliminado(true);
        entity.setFechaEliminacion(com.proveedores.time.MuseoTime.now());
        relacionObjetoRepository.save(entity);
    }

    private RelacionObjeto buscarActivo(Long id) {
        RelacionObjeto entity = relacionObjetoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Relacion entre objetos no encontrada"));
        if (entity.getEliminado()) {
            throw new ResourceNotFoundException("Relacion entre objetos no encontrada");
        }
        return entity;
    }

    private ObjetoMuseo buscarObjetoActivo(Long id) {
        ObjetoMuseo entity = objetoMuseoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Objeto de museo no encontrado"));
        if (Boolean.TRUE.equals(entity.getEliminado()) || Boolean.FALSE.equals(entity.getActivo())) {
            throw new ResourceNotFoundException("Objeto de museo no encontrado");
        }
        return entity;
    }

    private ObjetoMuseo buscarObjetoVisible(Long id) {
        ObjetoMuseo objeto = buscarObjetoActivo(id);
        if (!objetoVisible(objeto)) throw new ResourceNotFoundException("Objeto de museo no encontrado");
        return objeto;
    }

    private Veterano buscarPersonaActiva(Long id) {
        Veterano persona = veteranoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada"));
        if (!personaActiva(persona)) throw new ResourceNotFoundException("Persona no encontrada");
        return persona;
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
                    throw new ConflictException("Ya existe una relacion igual entre los objetos");
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

    private boolean relacionActivaConObjetosVisibles(RelacionObjeto relacion) {
        return !Boolean.TRUE.equals(relacion.getEliminado()) && objetoVisible(relacion.getObjetoOrigen()) && objetoVisible(relacion.getObjetoDestino());
    }

    private boolean relacionPersonaActiva(ObjetoVeterano relacion) {
        return !Boolean.TRUE.equals(relacion.getEliminado()) && objetoActivo(relacion.getObjetoMuseo()) && personaActiva(relacion.getVeterano());
    }

    private boolean objetoActivo(ObjetoMuseo objeto) {
        return objeto != null && !Boolean.TRUE.equals(objeto.getEliminado()) && !Boolean.FALSE.equals(objeto.getActivo());
    }

    private boolean personaActiva(Veterano persona) {
        return persona != null && !Boolean.TRUE.equals(persona.getEliminado()) && !Boolean.FALSE.equals(persona.getActivo());
    }

    private boolean objetoVisible(ObjetoMuseo objeto) {
        return objetoActivo(objeto) && ObjetoVisibilityPolicy.objetoVisible(objeto, embargoObjetoRepository);
    }

    private NodoGrafoObjetoDTO toNodo(ObjetoMuseo objeto) {
        return new NodoGrafoObjetoDTO(objetoId(objeto.getId()), objeto.getId(), TipoNodoRelacion.OBJETO, denominacionVisible(objeto), numeroInventarioVisible(objeto));
    }

    private NodoGrafoObjetoDTO toNodo(Veterano persona) {
        return new NodoGrafoObjetoDTO(personaId(persona.getId()), persona.getId(), TipoNodoRelacion.PERSONA, nombrePersona(persona), null);
    }

    private AristaGrafoObjetoDTO toArista(RelacionObjeto relacion) {
        return new AristaGrafoObjetoDTO(
                "OBJETO_RELACION-" + relacion.getId(),
                objetoId(relacion.getObjetoOrigen().getId()),
                objetoId(relacion.getObjetoDestino().getId()),
                TipoVinculoRelacion.OBJETO_OBJETO,
                relacion.getTipoRelacion(),
                relacion.getDescripcion()
        );
    }

    private AristaGrafoObjetoDTO toArista(ObjetoVeterano relacion) {
        return new AristaGrafoObjetoDTO("PERSONA_RELACION-" + relacion.getId(), personaId(relacion.getVeterano().getId()), objetoId(relacion.getObjetoMuseo().getId()), TipoVinculoRelacion.OBJETO_PERSONA, relacion.getTipoRelacion(), relacion.getDescripcion());
    }

    private RelacionElementoResponseDTO toElementoObjeto(RelacionObjeto relacion, Long objetoId) {
        boolean saliente = relacion.getObjetoOrigen().getId().equals(objetoId);
        ObjetoMuseo relacionado = saliente ? relacion.getObjetoDestino() : relacion.getObjetoOrigen();
        return new RelacionElementoResponseDTO("OBJETO_RELACION-" + relacion.getId(), TipoVinculoRelacion.OBJETO_OBJETO, TipoNodoRelacion.OBJETO, relacionado.getId(), numeroInventarioVisible(relacionado), denominacionVisible(relacionado), relacion.getTipoRelacion(), relacion.getDescripcion(), saliente ? "SALIENTE" : "ENTRANTE");
    }

    private RelacionElementoResponseDTO toElementoPersona(ObjetoVeterano relacion) {
        return new RelacionElementoResponseDTO("PERSONA_RELACION-" + relacion.getId(), TipoVinculoRelacion.OBJETO_PERSONA, TipoNodoRelacion.PERSONA, relacion.getVeterano().getId(), null, nombrePersona(relacion.getVeterano()), relacion.getTipoRelacion(), relacion.getDescripcion(), "VINCULADA");
    }

    private RelacionElementoResponseDTO toElementoObjetoDePersona(ObjetoVeterano relacion) {
        ObjetoMuseo objeto = relacion.getObjetoMuseo();
        return new RelacionElementoResponseDTO("PERSONA_RELACION-" + relacion.getId(), TipoVinculoRelacion.OBJETO_PERSONA, TipoNodoRelacion.OBJETO, objeto.getId(), numeroInventarioVisible(objeto), denominacionVisible(objeto), relacion.getTipoRelacion(), relacion.getDescripcion(), "VINCULADO");
    }

    private List<RelacionElementoResponseDTO> ordenarElementos(List<RelacionElementoResponseDTO> elementos) {
        return elementos.stream().sorted(Comparator.comparing(RelacionElementoResponseDTO::tipoElemento).thenComparing(RelacionElementoResponseDTO::denominacion, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private String denominacionVisible(ObjetoMuseo objeto) {
        return ObjetoVisibilityPolicy.campoVisible(objeto, "denominacionObjeto") ? objeto.getDenominacionObjeto() : "Objeto patrimonial";
    }

    private String numeroInventarioVisible(ObjetoMuseo objeto) {
        return ObjetoVisibilityPolicy.campoVisible(objeto, "numeroInventario") ? objeto.getNumeroInventario() : null;
    }

    private String nombrePersona(Veterano persona) {
        return (persona.getNombre() + " " + persona.getApellido()).trim();
    }

    private String objetoId(Long id) { return OBJETO_PREFIX + id; }
    private String personaId(Long id) { return PERSONA_PREFIX + id; }

    private static final class GrafoBuilder {
        private final Map<String, NodoGrafoObjetoDTO> nodos = new LinkedHashMap<>();
        private final Map<String, AristaGrafoObjetoDTO> aristas = new LinkedHashMap<>();
        void agregarNodo(NodoGrafoObjetoDTO nodo) { nodos.putIfAbsent(nodo.id(), nodo); }
        void agregarArista(AristaGrafoObjetoDTO arista) { aristas.putIfAbsent(arista.id(), arista); }
        ObjetoGrafoResponseDTO build() { return new ObjetoGrafoResponseDTO(new ArrayList<>(nodos.values()), new ArrayList<>(aristas.values())); }
    }
}
