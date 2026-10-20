export type MuseoRuntimeConfig = {
  apiBaseUrl: string;
  keycloakUrl: string;
  keycloakRealm: string;
  keycloakClientId: string;
};

declare global {
  interface Window { __MUSEO_RUNTIME_CONFIG__?: MuseoRuntimeConfig; }
}

export function getRuntimeConfig(): MuseoRuntimeConfig {
  if (typeof window !== "undefined" && window.__MUSEO_RUNTIME_CONFIG__) return window.__MUSEO_RUNTIME_CONFIG__;
  return { apiBaseUrl: "http://localhost:8080", keycloakUrl: "http://localhost:8081", keycloakRealm: "museo", keycloakClientId: "museo-frontend" };
}

export function runtimeConfigScript(config: MuseoRuntimeConfig): string {
  return JSON.stringify(config).replace(/</g, "\\u003c").replace(/>/g, "\\u003e").replace(/&/g, "\\u0026").replace(/\u2028/g, "\\u2028").replace(/\u2029/g, "\\u2029");
}
