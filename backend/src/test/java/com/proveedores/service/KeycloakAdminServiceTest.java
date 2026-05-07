package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.proveedores.config.KeycloakAdminProperties;
import com.proveedores.dto.AsignarRolRequestDTO;
import com.proveedores.dto.ResetPasswordRequestDTO;
import com.proveedores.dto.UsuarioKeycloakRequestDTO;
import com.proveedores.exception.BusinessException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeycloakAdminServiceTest {

    @Mock
    private Keycloak keycloak;
    @Mock
    private RealmResource realmResource;
    @Mock
    private UsersResource usersResource;
    @Mock
    private UserResource userResource;
    @Mock
    private RoleMappingResource roleMappingResource;
    @Mock
    private RoleScopeResource roleScopeResource;
    @Mock
    private RolesResource rolesResource;
    @Mock
    private RoleResource adminRoleResource;

    private KeycloakAdminService service;

    @BeforeEach
    void setUp() {
        KeycloakAdminProperties properties = new KeycloakAdminProperties(
                "http://localhost:8081",
                "museo",
                "museo-admin",
                "secret"
        );
        service = new KeycloakAdminService(keycloak, properties);
    }

    @Test
    void crearUsuarioConfiguraContrasenaInicialComoTemporal() {
        prepararRealm();
        when(usersResource.create(any(UserRepresentation.class)))
                .thenReturn(Response.created(URI.create("http://keycloak/admin/realms/museo/users/user-id")).build());
        prepararUsuarioExistente("user-id", "jperez", "Juan", "Perez", "jperez@local.test", List.of(), List.of(role("VIEWER")));
        prepararRolRealm("VIEWER");

        var response = service.crearUsuario(new UsuarioKeycloakRequestDTO(
                "jperez",
                "jperez@local.test",
                "Juan",
                "Perez",
                true,
                "Temporal123",
                Set.of("VIEWER")
        ));

        ArgumentCaptor<UserRepresentation> usuarioCaptor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(usersResource).create(usuarioCaptor.capture());
        CredentialRepresentation credencial = usuarioCaptor.getValue().getCredentials().get(0);
        assertThat(credencial.getType()).isEqualTo(CredentialRepresentation.PASSWORD);
        assertThat(credencial.getValue()).isEqualTo("Temporal123");
        assertThat(credencial.isTemporary()).isTrue();
        assertThat(response.id()).isEqualTo("user-id");
        assertThat(response.roles()).containsExactly("VIEWER");
    }

    @Test
    void asignarRolesReemplazaRolesGestionados() {
        prepararRealm();
        prepararUsuarioExistente(
                "user-id",
                "admin",
                "Admin",
                "Local",
                "admin@local.test",
                List.of(role("ADMIN"), role("VIEWER")),
                List.of(role("OPERATOR"))
        );
        prepararRolRealm("OPERATOR");

        var response = service.asignarRoles("user-id", new AsignarRolRequestDTO(Set.of("OPERATOR"), false), "other-admin-id");

        verify(roleScopeResource).remove(any());
        verify(roleScopeResource).add(any());
        assertThat(response.roles()).containsExactly("OPERATOR");
    }

    @Test
    void noPermiteQueAdminSeQuiteAdminSinConfirmacionExplicita() {
        assertThatThrownBy(() -> service.asignarRoles(
                "admin-id",
                new AsignarRolRequestDTO(Set.of("VIEWER"), false),
                "admin-id"
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("confirmacion explicita");
    }

    @Test
    void rechazaRolesFueraDelConjuntoPermitido() {
        assertThatThrownBy(() -> service.asignarRoles(
                "user-id",
                new AsignarRolRequestDTO(Set.of("SUPERUSER"), true),
                "admin-id"
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("ADMIN, OPERATOR o VIEWER");
    }

    @Test
    void resetearContrasenaSiempreUsaCredencialTemporal() {
        prepararRealm();
        when(usersResource.get("user-id")).thenReturn(userResource);

        service.resetearContrasenaTemporal("user-id", new ResetPasswordRequestDTO("NuevaClave123"));

        ArgumentCaptor<CredentialRepresentation> captor = ArgumentCaptor.forClass(CredentialRepresentation.class);
        verify(userResource).resetPassword(captor.capture());
        assertThat(captor.getValue().getValue()).isEqualTo("NuevaClave123");
        assertThat(captor.getValue().isTemporary()).isTrue();
    }

    private void prepararUsuarioExistente(
            String id,
            String username,
            String nombre,
            String apellido,
            String email,
            List<RoleRepresentation> rolesActuales,
            List<RoleRepresentation> rolesRespuesta
    ) {
        UserRepresentation usuario = new UserRepresentation();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setFirstName(nombre);
        usuario.setLastName(apellido);
        usuario.setEmail(email);
        usuario.setEnabled(true);
        when(usersResource.get(id)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(usuario);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        when(roleScopeResource.listAll()).thenReturn(rolesActuales, rolesRespuesta);
    }

    private void prepararRealm() {
        when(keycloak.realm("museo")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
    }

    private void prepararRolRealm(String nombre) {
        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get(nombre)).thenReturn(adminRoleResource);
        when(adminRoleResource.toRepresentation()).thenReturn(role(nombre));
    }

    private RoleRepresentation role(String nombre) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(nombre);
        return role;
    }
}
