"use client";

import Keycloak from "keycloak-js";

export const keycloakConfig = {
  url: process.env.NEXT_PUBLIC_KEYCLOAK_URL ?? "http://localhost:8081",
  realm: process.env.NEXT_PUBLIC_KEYCLOAK_REALM ?? "museo",
  clientId: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID ?? "museo-frontend"
};

export const keycloak = new Keycloak(keycloakConfig);
