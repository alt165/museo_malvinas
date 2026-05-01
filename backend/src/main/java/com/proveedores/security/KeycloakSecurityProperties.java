package com.proveedores.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.keycloak")
public record KeycloakSecurityProperties(String clientId) {
}
