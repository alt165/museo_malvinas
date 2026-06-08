package com.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proveedores.entity.Auditoria;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.TipoOperacionAuditoria;
import com.proveedores.repository.AuditoriaRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AuditoriaObjetoServiceTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

    private AuditoriaObjetoService service;

    @BeforeEach
    void setUp() {
        service = new AuditoriaObjetoService(auditoriaRepository, new ObjectMapper().findAndRegisterModules());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registrarUsaUsuarioAutenticadoDelJwt() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject-id")
                .claim("preferred_username", "admin")
                .claim("name", "Administrador Museo")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
        ObjetoMuseo objeto = new ObjetoMuseo();
        objeto.setId(7L);
        objeto.setNumeroInventario("INV-7");

        service.registrar(
                objeto,
                TipoOperacionAuditoria.CREACION,
                "ALTA_COMPLETA",
                "Alta completa de objeto",
                "ALTA_COMPLETA",
                null,
                service.mapOf("numeroInventario", "INV-7"),
                null
        );

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        Auditoria auditoria = captor.getValue();
        assertThat(auditoria.getEntidad()).isEqualTo(AuditoriaObjetoService.ENTIDAD_OBJETO);
        assertThat(auditoria.getEntidadId()).isEqualTo(7L);
        assertThat(auditoria.getUsuarioIdentificador()).isEqualTo("admin");
        assertThat(auditoria.getUsuarioNombre()).isEqualTo("Administrador Museo");
        assertThat(auditoria.getRol()).isEqualTo("ADMIN");
        assertThat(auditoria.getDatosNuevos()).contains("INV-7");
    }

    @Test
    void listarHistorialMapeaEventosEnOrdenDelRepositorio() {
        Auditoria auditoria = new Auditoria();
        auditoria.setId(1L);
        auditoria.setFecha(LocalDateTime.of(2026, 6, 8, 11, 0));
        auditoria.setTipoOperacion(TipoOperacionAuditoria.MODIFICACION);
        auditoria.setAccion("COMPLETAR_CARGA");
        auditoria.setDescripcion("Completar carga de objeto creado por alta rápida");
        auditoria.setOrigen("COMPLETAR_CARGA");
        auditoria.setUsuarioNombre("admin");
        auditoria.setRol("ADMIN");
        auditoria.setDatosPrevios("{}");
        auditoria.setDatosNuevos("{\"datosCompletos\":true}");
        when(auditoriaRepository.findByEntidadAndEntidadIdOrderByFechaDesc(AuditoriaObjetoService.ENTIDAD_OBJETO, 7L))
                .thenReturn(List.of(auditoria));

        var historial = service.listarHistorial(7L);

        assertThat(historial).hasSize(1);
        assertThat(historial.get(0).accion()).isEqualTo("COMPLETAR_CARGA");
        assertThat(historial.get(0).usuario()).isEqualTo("admin");
        assertThat(historial.get(0).valoresNuevos()).contains("datosCompletos");
        verify(auditoriaRepository).findByEntidadAndEntidadIdOrderByFechaDesc(AuditoriaObjetoService.ENTIDAD_OBJETO, 7L);
    }
}
