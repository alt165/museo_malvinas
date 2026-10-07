package com.proveedores.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtTokenValidatorTest {

    private static final String ISSUER = "http://localhost:8081/realms/museo";
    private final SecurityConfig securityConfig = new SecurityConfig(null, new ObjectMapper());
    private final OAuth2TokenValidator<Jwt> validator = securityConfig.jwtTokenValidator(
            ISSUER, new KeycloakSecurityProperties("museo-backend"));

    @Test
    void aceptaIssuerYAudienciaCorrectos() {
        assertThat(validator.validate(token(ISSUER, List.of("account", "museo-backend"))).hasErrors()).isFalse();
    }

    @Test
    void rechazaIssuerIncorrecto() {
        assertThat(validator.validate(token("http://otro-issuer", List.of("museo-backend"))).hasErrors()).isTrue();
    }

    @Test
    void rechazaAudienciaDeOtroCliente() {
        assertThat(validator.validate(token(ISSUER, List.of("otro-cliente"))).hasErrors()).isTrue();
    }

    @Test
    void rechazaTokenSinAudienciaRequerida() {
        assertThat(validator.validate(token(ISSUER, List.of())).hasErrors()).isTrue();
    }

    @Test
    void converterConservaRolesConTokenDeAudienciaCorrecta() {
        Jwt jwt = token(ISSUER, List.of("museo-backend"));
        Jwt withRoles = Jwt.withTokenValue(jwt.getTokenValue())
                .headers(headers -> headers.putAll(jwt.getHeaders()))
                .claims(claims -> {
                    claims.putAll(jwt.getClaims());
                    claims.put("realm_access", Map.of("roles", List.of("ADMIN", "MUSEOLOGO", "VIEWER")));
                })
                .issuedAt(jwt.getIssuedAt())
                .expiresAt(jwt.getExpiresAt())
                .build();

        var authentication = new KeycloakJwtAuthenticationConverter(
                new KeycloakSecurityProperties("museo-backend")).convert(withRoles);

        assertThat(authentication.getAuthorities()).extracting("authority")
                .contains("ROLE_ADMIN", "ROLE_MUSEOLOGO", "ROLE_VIEWER");
    }

    private Jwt token(String issuer, List<String> audience) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("tester")
                .issuer(issuer)
                .audience(audience)
                .issuedAt(now.minusSeconds(30))
                .expiresAt(now.plusSeconds(300))
                .build();
    }
}
