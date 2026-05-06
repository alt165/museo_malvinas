"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { KeycloakTokenParsed } from "keycloak-js";
import type { AuthSession, SessionUser, UserRole } from "@/models/session";
import { keycloak, keycloakConfig } from "./keycloak";
import { setLoginHandler, setTokenProvider } from "./session";

type AuthContextValue = AuthSession & {
  getToken: () => Promise<string | null>;
  login: () => Promise<void>;
  logout: () => Promise<void>;
  roles: UserRole[];
};

const AuthContext = createContext<AuthContextValue | null>(null);
let keycloakInitPromise: Promise<boolean> | null = null;

type MuseoTokenParsed = KeycloakTokenParsed & {
  email?: string;
  name?: string;
  preferred_username?: string;
  realm_access?: {
    roles?: string[];
  };
  resource_access?: Record<string, { roles?: string[] }>;
};

function isUserRole(role: string): role is UserRole {
  return role === "ADMIN" || role === "OPERATOR" || role === "VIEWER";
}

function getRoles(token?: MuseoTokenParsed) {
  const realmRoles = token?.realm_access?.roles ?? [];
  const frontendRoles = token?.resource_access?.[keycloakConfig.clientId]?.roles ?? [];
  const backendRoles = token?.resource_access?.["museo-backend"]?.roles ?? [];

  return Array.from(new Set([...realmRoles, ...frontendRoles, ...backendRoles].filter(isUserRole)));
}

function getUserFromToken(token?: MuseoTokenParsed): SessionUser | null {
  if (!token) {
    return null;
  }

  return {
    id: token.sub,
    username: token.preferred_username,
    email: token.email,
    name: token.name ?? token.preferred_username,
    roles: getRoles(token)
  };
}

function initKeycloak() {
  keycloakInitPromise ??= keycloak.init({
    onLoad: "check-sso",
    pkceMethod: "S256",
    checkLoginIframe: false
  });

  return keycloakInitPromise;
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [loading, setLoading] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);
  const [user, setUser] = useState<SessionUser | null>(null);

  const syncSession = useCallback(() => {
    const nextAuthenticated = Boolean(keycloak.authenticated);
    setAuthenticated(nextAuthenticated);
    setUser(nextAuthenticated ? getUserFromToken(keycloak.tokenParsed as MuseoTokenParsed | undefined) : null);
  }, []);

  const login = useCallback(async () => {
    await keycloak.login({
      redirectUri: `${window.location.origin}/dashboard`
    });
  }, []);

  const logout = useCallback(async () => {
    setAuthenticated(false);
    setUser(null);
    await keycloak.logout({
      redirectUri: window.location.origin
    });
  }, []);

  const getToken = useCallback(async () => {
    if (!keycloak.authenticated) {
      return null;
    }

    try {
      await keycloak.updateToken(30);
      syncSession();
      return keycloak.token ?? null;
    } catch {
      await login();
      return null;
    }
  }, [login, syncSession]);

  useEffect(() => {
    let mounted = true;

    setTokenProvider(getToken);
    setLoginHandler(login);

    keycloak.onTokenExpired = () => {
      void keycloak.updateToken(60).then(syncSession).catch(login);
    };

    initKeycloak()
      .then(() => {
        if (!mounted) {
          return;
        }

        syncSession();
      })
      .finally(() => {
        if (mounted) {
          setLoading(false);
        }
      });

    return () => {
      mounted = false;
      setTokenProvider(null);
      setLoginHandler(null);
      keycloak.onTokenExpired = undefined;
    };
  }, [getToken, login, syncSession]);

  const value = useMemo<AuthContextValue>(
    () => ({
      authenticated,
      loading,
      user,
      roles: user?.roles ?? [],
      getToken,
      login,
      logout
    }),
    [authenticated, getToken, loading, login, logout, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
}
