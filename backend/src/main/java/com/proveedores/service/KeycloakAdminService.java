package com.proveedores.service;

import com.proveedores.config.KeycloakAdminProperties;
import com.proveedores.dto.AsignarRolRequestDTO;
import com.proveedores.dto.ResetPasswordRequestDTO;
import com.proveedores.dto.UsuarioKeycloakRequestDTO;
import com.proveedores.dto.UsuarioKeycloakResponseDTO;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

@Service
public class KeycloakAdminService {

    private static final Set<String> ROLES_GESTIONABLES = Set.of("ADMIN", "OPERATOR", "VIEWER");
    private static final String DNI_ATTRIBUTE = "dni";
    private static final String UPDATE_PASSWORD_REQUIRED_ACTION = "UPDATE_PASSWORD";

    private final Keycloak keycloak;
    private final KeycloakAdminProperties properties;

    public KeycloakAdminService(Keycloak keycloak, KeycloakAdminProperties properties) {
        this.keycloak = keycloak;
        this.properties = properties;
    }

    public List<UsuarioKeycloakResponseDTO> listarUsuarios() {
        return usersResource().list().stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(UsuarioKeycloakResponseDTO::username, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    public UsuarioKeycloakResponseDTO obtenerUsuario(String id) {
        return toResponse(obtenerRepresentacion(id));
    }

    public UsuarioKeycloakResponseDTO crearUsuario(UsuarioKeycloakRequestDTO dto) {
        validarRoles(dto.roles());

        UserRepresentation usuario = new UserRepresentation();
        usuario.setUsername(dto.username());
        usuario.setEmail(dto.email());
        usuario.setFirstName(dto.nombre());
        usuario.setLastName(dto.apellido());
        usuario.setEnabled(dto.habilitado() == null || dto.habilitado());
        usuario.setEmailVerified(false);
        setDniAttribute(usuario, dto.dni());
        usuario.setRequiredActions(List.of(UPDATE_PASSWORD_REQUIRED_ACTION));
        if (dto.contrasenaInicial() != null && !dto.contrasenaInicial().isBlank()) {
            usuario.setCredentials(List.of(crearCredencialTemporal(dto.contrasenaInicial())));
        }

        try (Response response = usersResource().create(usuario)) {
            if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
                String id = extraerIdCreado(response.getLocation());
                try {
                    actualizarDniUsuarioCreado(id, dto);
                    if (dto.roles() != null && !dto.roles().isEmpty()) {
                        return asignarRoles(id, new AsignarRolRequestDTO(dto.roles(), true), null);
                    }
                    return obtenerUsuario(id);
                } catch (RuntimeException exception) {
                    eliminarUsuarioCreado(id);
                    throw exception;
                }
            }
            if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new BusinessException("Ya existe un usuario con ese username o email");
            }
            throw new BusinessException("No se pudo crear el usuario en Keycloak");
        }
    }

    public UsuarioKeycloakResponseDTO actualizarDatosBasicos(String id, UsuarioKeycloakRequestDTO dto) {
        UserResource userResource = userResource(id);
        UserRepresentation usuario = obtenerRepresentacion(id);
        usuario.setUsername(dto.username());
        usuario.setEmail(dto.email());
        usuario.setFirstName(dto.nombre());
        usuario.setLastName(dto.apellido());
        setDniAttribute(usuario, dto.dni());
        if (dto.habilitado() != null) {
            usuario.setEnabled(dto.habilitado());
        }
        ejecutarOperacionKeycloak(() -> userResource.update(usuario));
        return obtenerUsuario(id);
    }

    public UsuarioKeycloakResponseDTO cambiarEstado(String id, boolean habilitado) {
        UserResource userResource = userResource(id);
        UserRepresentation usuario = obtenerRepresentacion(id);
        usuario.setEnabled(habilitado);
        ejecutarOperacionKeycloak(() -> userResource.update(usuario));
        return obtenerUsuario(id);
    }

    public void resetearContrasenaTemporal(String id, ResetPasswordRequestDTO dto) {
        ejecutarOperacionKeycloak(() -> userResource(id).resetPassword(crearCredencialTemporal(dto.contrasena())));
    }

    public UsuarioKeycloakResponseDTO asignarRoles(String id, AsignarRolRequestDTO dto, String administradorActualId) {
        Set<String> rolesSolicitados = validarRoles(dto.roles());
        if (Objects.equals(id, administradorActualId)
                && !rolesSolicitados.contains("ADMIN")
                && !Boolean.TRUE.equals(dto.confirmarQuitarAdminPropio())) {
            throw new BusinessException("Para quitarte el rol ADMIN debes enviar confirmacion explicita");
        }

        RoleScopeResource realmRoles = userResource(id).roles().realmLevel();
        List<RoleRepresentation> rolesActualesGestionados = realmRoles.listAll().stream()
                .filter(role -> ROLES_GESTIONABLES.contains(role.getName()))
                .toList();
        if (!rolesActualesGestionados.isEmpty()) {
            ejecutarOperacionKeycloak(() -> realmRoles.remove(rolesActualesGestionados));
        }

        List<RoleRepresentation> rolesNuevos = obtenerRepresentacionesRolesRealm(rolesSolicitados);
        if (!rolesNuevos.isEmpty()) {
            ejecutarOperacionKeycloak(() -> realmRoles.add(rolesNuevos));
        }

        return obtenerUsuario(id);
    }

    private void actualizarDniUsuarioCreado(String id, UsuarioKeycloakRequestDTO dto) {
        UserResource userResource = userResource(id);
        UserRepresentation usuario = obtenerRepresentacion(id);
        setDniAttribute(usuario, dto.dni());
        ejecutarOperacionKeycloak(() -> userResource.update(usuario));
    }

    private void eliminarUsuarioCreado(String id) {
        try {
            userResource(id).remove();
        } catch (WebApplicationException exception) {
            // La operacion original es mas relevante para el cliente que una limpieza fallida.
        }
    }

    private List<RoleRepresentation> obtenerRepresentacionesRolesRealm(Set<String> rolesSolicitados) {
        try {
            return rolesSolicitados.stream()
                    .map(role -> realmResource().roles().get(role).toRepresentation())
                    .toList();
        } catch (WebApplicationException exception) {
            throw new BusinessException("No se pudieron consultar los roles en Keycloak");
        }
    }

    private UserRepresentation obtenerRepresentacion(String id) {
        try {
            return userResource(id).toRepresentation();
        } catch (WebApplicationException exception) {
            if (exception.getResponse() != null && exception.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new ResourceNotFoundException("Usuario de Keycloak no encontrado");
            }
            throw new BusinessException("No se pudo consultar el usuario en Keycloak");
        }
    }

    private UsuarioKeycloakResponseDTO toResponse(UserRepresentation usuario) {
        return new UsuarioKeycloakResponseDTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                getDniAttribute(usuario),
                usuario.getFirstName(),
                usuario.getLastName(),
                usuario.isEnabled(),
                obtenerRolesGestionados(usuario.getId())
        );
    }

    private Set<String> obtenerRolesGestionados(String usuarioId) {
        if (usuarioId == null) {
            return Set.of();
        }
        try {
            return userResource(usuarioId).roles().realmLevel().listAll().stream()
                    .map(RoleRepresentation::getName)
                    .filter(ROLES_GESTIONABLES::contains)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        } catch (WebApplicationException exception) {
            return Set.of();
        }
    }

    private void setDniAttribute(UserRepresentation usuario, String dni) {
        Map<String, List<String>> attributes = usuario.getAttributes();
        attributes = attributes == null ? new java.util.HashMap<>() : new java.util.HashMap<>(attributes);
        attributes.put(DNI_ATTRIBUTE, List.of(dni.trim()));
        usuario.setAttributes(attributes);
    }

    private String getDniAttribute(UserRepresentation usuario) {
        List<String> values = usuario.getAttributes() == null ? null : usuario.getAttributes().get(DNI_ATTRIBUTE);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private Set<String> validarRoles(Set<String> roles) {
        if (roles == null) {
            return Set.of();
        }
        Set<String> rolesNormalizados = roles.stream()
                .filter(Objects::nonNull)
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .filter(role -> !role.isBlank())
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        if (!ROLES_GESTIONABLES.containsAll(rolesNormalizados)) {
            throw new BusinessException("Solo se pueden asignar los roles ADMIN, OPERATOR o VIEWER");
        }
        return rolesNormalizados;
    }

    private CredentialRepresentation crearCredencialTemporal(String contrasena) {
        CredentialRepresentation credencial = new CredentialRepresentation();
        credencial.setType(CredentialRepresentation.PASSWORD);
        credencial.setValue(contrasena);
        credencial.setTemporary(true);
        return credencial;
    }

    private String extraerIdCreado(URI location) {
        if (location == null || location.getPath() == null || location.getPath().isBlank()) {
            throw new BusinessException("Keycloak no devolvio el id del usuario creado");
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private void ejecutarOperacionKeycloak(Runnable operation) {
        try {
            operation.run();
        } catch (WebApplicationException exception) {
            if (exception.getResponse() != null && exception.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new ResourceNotFoundException("Usuario de Keycloak no encontrado");
            }
            if (exception.getResponse() != null && exception.getResponse().getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new BusinessException("La operacion entra en conflicto con datos existentes en Keycloak");
            }
            throw new BusinessException("No se pudo completar la operacion en Keycloak");
        }
    }

    private RealmResource realmResource() {
        return keycloak.realm(properties.realm());
    }

    private UsersResource usersResource() {
        return realmResource().users();
    }

    private UserResource userResource(String id) {
        return usersResource().get(id);
    }
}
