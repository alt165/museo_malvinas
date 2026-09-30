package com.proveedores.service;

import com.proveedores.entity.Usuario;
import com.proveedores.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UsuarioMovimientoService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioMovimientoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Optional<Usuario> resolver(String identificadorFallback) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            String keycloakId = jwtAuthentication.getToken().getSubject();
            String username = jwtAuthentication.getToken().getClaimAsString("preferred_username");
            String email = jwtAuthentication.getToken().getClaimAsString("email");

            Optional<Usuario> existente = usuarioRepository.findByKeycloakIdAndEliminadoFalse(keycloakId);
            if (existente.isEmpty() && StringUtils.hasText(email)) {
                existente = usuarioRepository.findByEmailAndEliminadoFalse(email);
            }
            if (existente.isPresent()) {
                return existente;
            }
            if (StringUtils.hasText(keycloakId) && StringUtils.hasText(username) && StringUtils.hasText(email)) {
                Usuario usuario = new Usuario();
                usuario.setKeycloakId(keycloakId);
                usuario.setNombre(username);
                usuario.setEmail(email);
                usuario.setFechaCreacion(com.proveedores.time.MuseoTime.now());
                return Optional.of(usuarioRepository.save(usuario));
            }
        }

        if (StringUtils.hasText(identificadorFallback)) {
            return usuarioRepository.findByEmailAndEliminadoFalse(identificadorFallback);
        }
        return Optional.empty();
    }
}
