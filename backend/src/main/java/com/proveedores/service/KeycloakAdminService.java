package com.proveedores.service;

import com.proveedores.config.KeycloakAdminProperties;
import com.proveedores.dto.AsignarRolRequestDTO;
import com.proveedores.dto.ResetPasswordRequestDTO;
import com.proveedores.dto.UsuarioKeycloakRequestDTO;
import com.proveedores.dto.UsuarioKeycloakResponseDTO;
import com.proveedores.exception.BusinessException;
import com.proveedores.exception.ResourceNotFoundException;
import com.proveedores.entity.TipoOperacionAuditoria;
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

    private static final Set<String> ROLES_GESTIONABLES = Set.of("ADMIN", "MUSEOLOGO", "VIEWER");
    private static final String DNI_ATTRIBUTE = "dni";
    private static final String UPDATE_PASSWORD_REQUIRED_ACTION = "UPDATE_PASSWORD";

    private final Keycloak keycloak;
    private final KeycloakAdminProperties properties;
    private final AuditoriaObjetoService auditoriaService;

    public KeycloakAdminService(Keycloak keycloak, KeycloakAdminProperties properties, AuditoriaObjetoService auditoriaService) {
        this.keycloak = keycloak;
        this.properties = properties;
        this.auditoriaService = auditoriaService;
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
                    UsuarioKeycloakResponseDTO creado = dto.roles() != null && !dto.roles().isEmpty()
                            ? asignarRolesInterno(id, new AsignarRolRequestDTO(dto.roles(), true), null)
                            : obtenerUsuario(id);
                    auditarExito(id, TipoOperacionAuditoria.CREACION, "USUARIO_CREADO", "Creación de usuario en Keycloak", null,
                            auditoriaService.mapOf("username", dto.username(), "email", dto.email(), "dni", dto.dni(), "roles", creado.roles()));
                    return creado;
                } catch (RuntimeException exception) {
                    eliminarUsuarioCreado(id);
                    auditarFallo(id, "USUARIO_CREACION_FALLIDA", exception);
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
        try {
            UserResource userResource = userResource(id);
            UserRepresentation usuario = obtenerRepresentacion(id);
            Map<String, Object> anterior = auditoriaService.mapOf(
                    "username", usuario.getUsername(), "email", usuario.getEmail(), "habilitado", usuario.isEnabled());
            usuario.setUsername(dto.username());
            usuario.setEmail(dto.email());
            usuario.setFirstName(dto.nombre());
            usuario.setLastName(dto.apellido());
            setDniAttribute(usuario, dto.dni());
            if (dto.habilitado() != null) {
                usuario.setEnabled(dto.habilitado());
            }
            ejecutarOperacionKeycloak(() -> userResource.update(usuario));
            UsuarioKeycloakResponseDTO actualizado = obtenerUsuario(id);
            auditarExito(id, "USUARIO_ACTUALIZADO", "Actualización de datos de usuario en Keycloak", anterior,
                    auditoriaService.mapOf("username", actualizado.username(), "email", actualizado.email(), "habilitado", actualizado.habilitado()));
            return actualizado;
        } catch (RuntimeException exception) {
            auditarFallo(id, "USUARIO_ACTUALIZACION_FALLIDA", exception);
            throw exception;
        }
    }

    public UsuarioKeycloakResponseDTO cambiarEstado(String id, boolean habilitado) {
        try {
            UserResource userResource = userResource(id);
            UserRepresentation usuario = obtenerRepresentacion(id);
            boolean anterior = Boolean.TRUE.equals(usuario.isEnabled());
            usuario.setEnabled(habilitado);
            ejecutarOperacionKeycloak(() -> userResource.update(usuario));
            UsuarioKeycloakResponseDTO actualizado = obtenerUsuario(id);
            auditarExito(id, habilitado ? "USUARIO_HABILITADO" : "USUARIO_DESHABILITADO",
                    "Cambio de estado de usuario en Keycloak",
                    auditoriaService.mapOf("habilitado", anterior), auditoriaService.mapOf("habilitado", habilitado));
            return actualizado;
        } catch (RuntimeException exception) {
            auditarFallo(id, "USUARIO_ESTADO_FALLIDO", exception);
            throw exception;
        }
    }

    public void resetearContrasenaTemporal(String id, ResetPasswordRequestDTO dto) {
        try {
            ejecutarOperacionKeycloak(() -> userResource(id).resetPassword(crearCredencialTemporal(dto.contrasena())));
            auditarExito(id, "PASSWORD_TEMPORAL_RESETEADA", "Reset de contraseña temporal en Keycloak", null,
                    auditoriaService.mapOf("credencialTemporalConfigurada", true));
        } catch (RuntimeException exception) {
            auditarFallo(id, "PASSWORD_RESET_FALLIDO", exception);
            throw exception;
        }
    }

    public UsuarioKeycloakResponseDTO asignarRoles(String id, AsignarRolRequestDTO dto, String administradorActualId) {
        try {
            UsuarioKeycloakResponseDTO resultado = asignarRolesInterno(id, dto, administradorActualId);
            return resultado;
        } catch (RuntimeException exception) {
            auditarFallo(id, "ROLES_CAMBIO_FALLIDO", exception);
            throw exception;
        }
    }

    private UsuarioKeycloakResponseDTO asignarRolesInterno(String id, AsignarRolRequestDTO dto, String administradorActualId) {
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
        Set<String> nombresActuales = nombresRoles(rolesActualesGestionados);
        List<RoleRepresentation> rolesNuevos = obtenerRepresentacionesRolesRealm(rolesSolicitados);
        List<RoleRepresentation> rolesAAgregar = rolesNuevos.stream()
                .filter(role -> !nombresActuales.contains(role.getName()))
                .toList();
        List<RoleRepresentation> rolesAQuitar = rolesActualesGestionados.stream()
                .filter(role -> !rolesSolicitados.contains(role.getName()))
                .toList();

        if (!rolesAAgregar.isEmpty()) {
            ejecutarOperacionKeycloak(() -> realmRoles.add(rolesAAgregar));
            Set<String> despuesDeAgregar = nombresRolesGestionados(realmRoles.listAll());
            if (!despuesDeAgregar.containsAll(rolesSolicitados)) {
                throw new BusinessException("Keycloak no confirmó la asignación de todos los roles solicitados");
            }
        }
        if (!rolesAQuitar.isEmpty()) {
            ejecutarOperacionKeycloak(() -> realmRoles.remove(rolesAQuitar));
        }
        Set<String> estadoFinal = nombresRolesGestionados(realmRoles.listAll());
        if (!estadoFinal.equals(rolesSolicitados)) {
            throw new BusinessException("Keycloak no confirmó el estado final de roles del usuario");
        }
        UsuarioKeycloakResponseDTO resultado = obtenerUsuario(id);
        auditarExito(id, "ROLES_ACTUALIZADOS", "Cambio de roles de usuario en Keycloak",
                auditoriaService.mapOf("roles", nombresActuales), auditoriaService.mapOf("roles", rolesSolicitados));
        return resultado;
    }

    private Set<String> nombresRoles(List<RoleRepresentation> roles) {
        return roles.stream().map(RoleRepresentation::getName)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> nombresRolesGestionados(List<RoleRepresentation> roles) {
        return nombresRoles(roles).stream().filter(ROLES_GESTIONABLES::contains)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private void auditarExito(String usuarioId, String accion, String descripcion, Object anterior, Object nuevo) {
        auditarExito(usuarioId, TipoOperacionAuditoria.MODIFICACION, accion, descripcion, anterior, nuevo);
    }

    private void auditarExito(String usuarioId, TipoOperacionAuditoria tipoOperacion, String accion,
                              String descripcion, Object anterior, Object nuevo) {
        auditoriaService.registrarEventoIndependiente("USUARIO_KEYCLOAK", null, usuarioId,
                tipoOperacion, accion, descripcion, "KEYCLOAK", anterior, nuevo, null);
    }

    private void auditarFallo(String usuarioId, String accion, RuntimeException exception) {
        try {
            auditoriaService.registrarEventoIndependiente("USUARIO_KEYCLOAK", null, usuarioId,
                    TipoOperacionAuditoria.MODIFICACION, accion,
                    "La operación de administración en Keycloak no se completó", "KEYCLOAK",
                    null, auditoriaService.mapOf("resultado", "FALLO", "tipoError", exception.getClass().getSimpleName()), null);
        } catch (RuntimeException auditException) {
            // La causa original de Keycloak conserva prioridad; el fallo de auditoría queda en logs del handler.
        }
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
            throw new BusinessException("Solo se pueden asignar los roles ADMIN, MUSEOLOGO o VIEWER");
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
