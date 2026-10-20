"use client";

import Keycloak from "keycloak-js";
import { getRuntimeConfig } from "@/lib/runtime-config";

export function getKeycloakConfig() {
  const runtime = getRuntimeConfig();
  return { url: runtime.keycloakUrl, realm: runtime.keycloakRealm, clientId: runtime.keycloakClientId };
}

let instance: Keycloak | null = null;
export function getKeycloak(): Keycloak { instance ??= new Keycloak(getKeycloakConfig()); return instance; }
