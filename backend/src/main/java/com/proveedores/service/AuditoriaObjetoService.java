package com.proveedores.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proveedores.dto.HistorialObjetoResponseDTO;
import com.proveedores.entity.Auditoria;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.TipoOperacionAuditoria;
import com.proveedores.repository.AuditoriaRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuditoriaObjetoService {

    public static final String ENTIDAD_OBJETO = "OBJETO_MUSEO";

    private final AuditoriaRepository auditoriaRepository;
    private final ObjectMapper objectMapper;

    public AuditoriaObjetoService(AuditoriaRepository auditoriaRepository, ObjectMapper objectMapper) {
        this.auditoriaRepository = auditoriaRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void registrar(
            ObjetoMuseo objeto,
            TipoOperacionAuditoria tipoOperacion,
            String accion,
            String descripcion,
            String origen,
            Object valoresAnteriores,
            Object valoresNuevos,
            String usuarioFallback
    ) {
        if (objeto == null || objeto.getId() == null) {
            return;
        }

        ActorAuditoria actor = actorActual(usuarioFallback);

        Auditoria auditoria = new Auditoria();
        auditoria.setFecha(LocalDateTime.now());
        auditoria.setTipoOperacion(tipoOperacion);
        auditoria.setEntidad(ENTIDAD_OBJETO);
        auditoria.setEntidadId(objeto.getId());
        auditoria.setNumeroInventario(objeto.getNumeroInventario());
        auditoria.setAccion(accion);
        auditoria.setDescripcion(descripcion);
        auditoria.setOrigen(origen);
        auditoria.setUsuarioIdentificador(actor.identificador());
        auditoria.setUsuarioNombre(actor.nombreVisible());
        auditoria.setRol(actor.rol());
        auditoria.setDatosPrevios(serializar(valoresAnteriores));
        auditoria.setDatosNuevos(serializar(valoresNuevos));
        auditoriaRepository.save(auditoria);
    }

    @Transactional(readOnly = true)
    public List<HistorialObjetoResponseDTO> listarHistorial(Long objetoId) {
        return auditoriaRepository.findByEntidadAndEntidadIdOrderByFechaDesc(ENTIDAD_OBJETO, objetoId).stream()
                .map(this::toResponse)
                .toList();
    }

    public Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private HistorialObjetoResponseDTO toResponse(Auditoria auditoria) {
        return new HistorialObjetoResponseDTO(
                auditoria.getId(),
                auditoria.getFecha(),
                auditoria.getTipoOperacion().name(),
                auditoria.getAccion(),
                auditoria.getDescripcion(),
                usuarioVisible(auditoria),
                auditoria.getRol(),
                auditoria.getOrigen(),
                auditoria.getDatosPrevios(),
                auditoria.getDatosNuevos()
        );
    }

    private String usuarioVisible(Auditoria auditoria) {
        if (StringUtils.hasText(auditoria.getUsuarioNombre())) {
            return auditoria.getUsuarioNombre();
        }
        if (StringUtils.hasText(auditoria.getUsuarioIdentificador())) {
            return auditoria.getUsuarioIdentificador();
        }
        return "Usuario no identificado";
    }

    private String serializar(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return String.valueOf(value);
        }
    }

    private ActorAuditoria actorActual(String usuarioFallback) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            String preferredUsername = jwtAuthentication.getToken().getClaimAsString("preferred_username");
            String email = jwtAuthentication.getToken().getClaimAsString("email");
            String name = jwtAuthentication.getToken().getClaimAsString("name");
            String subject = jwtAuthentication.getToken().getSubject();
            String identificador = firstText(preferredUsername, email, subject, usuarioFallback, "Usuario no identificado");
            String nombreVisible = firstText(name, preferredUsername, email, usuarioFallback, "Usuario no identificado");
            return new ActorAuditoria(identificador, nombreVisible, rol(authentication.getAuthorities()));
        }
        if (authentication != null && StringUtils.hasText(authentication.getName())) {
            return new ActorAuditoria(authentication.getName(), authentication.getName(), rol(authentication.getAuthorities()));
        }
        String usuario = firstText(usuarioFallback, "Usuario no identificado");
        return new ActorAuditoria(usuario, usuario, null);
    }

    private String rol(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) {
            return null;
        }
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse(null);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private record ActorAuditoria(String identificador, String nombreVisible, String rol) {
    }
}
