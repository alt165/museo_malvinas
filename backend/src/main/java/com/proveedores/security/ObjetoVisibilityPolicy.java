package com.proveedores.security;

import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.VisibilidadCampo;
import com.proveedores.repository.EmbargoObjetoRepository;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class ObjetoVisibilityPolicy {

    private ObjetoVisibilityPolicy() {
    }

    public static boolean puedeVerCamposPrivados() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority)
                        || "ROLE_OPERATOR".equals(authority)
                        || "ROLE_MUSEOLOGO".equals(authority));
    }

    public static boolean campoVisible(ObjetoMuseo objeto, String campo) {
        if (puedeVerCamposPrivados()) {
            return true;
        }
        Map<String, VisibilidadCampo> visibilidades = objeto.getVisibilidades() == null ? Map.of() : objeto.getVisibilidades();
        return visibilidades.getOrDefault(campo, VisibilidadCampo.PUBLICO) != VisibilidadCampo.PRIVADO;
    }

    public static boolean objetoVisible(ObjetoMuseo objeto, EmbargoObjetoRepository embargoObjetoRepository) {
        return puedeVerCamposPrivados()
                || !embargoObjetoRepository.existsByObjetoMuseoIdAndFechaFinalizacionIsNullAndEliminadoFalse(objeto.getId());
    }
}
