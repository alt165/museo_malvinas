package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.proveedores.entity.Usuario;
import com.proveedores.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class UsuarioMovimientoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioMovimientoService service;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reutilizaUsuarioExistentePorKeycloakId() {
        Usuario usuario = new Usuario();
        usuario.setKeycloakId("keycloak-1");
        usuario.setNombre("admin.local");
        autenticar("keycloak-1", "admin.local", "admin@example.test");
        when(usuarioRepository.findByKeycloakIdAndEliminadoFalse("keycloak-1")).thenReturn(Optional.of(usuario));

        assertThat(service.resolver("admin.local")).containsSame(usuario);
    }

    @Test
    void registraReferenciaLocalParaUsuarioAutenticadoNuevo() {
        autenticar("keycloak-2", "operador.local", "operador@example.test");
        when(usuarioRepository.findByKeycloakIdAndEliminadoFalse("keycloak-2")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailAndEliminadoFalse("operador@example.test")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario resultado = service.resolver("operador.local").orElseThrow();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(resultado.getKeycloakId()).isEqualTo("keycloak-2");
        assertThat(resultado.getNombre()).isEqualTo("operador.local");
        assertThat(resultado.getEmail()).isEqualTo("operador@example.test");
    }

    private void autenticar(String subject, String username, String email) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .claim("preferred_username", username)
                .claim("email", email)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
