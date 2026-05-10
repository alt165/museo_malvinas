package com.proveedores.dto;

import java.util.Set;

public record UsuarioKeycloakResponseDTO(
        String id,
        String username,
        String email,
        String dni,
        String nombre,
        String apellido,
        Boolean habilitado,
        Set<String> roles
) {
}
