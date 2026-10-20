"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { KeycloakTokenParsed } from "keycloak-js";
import type { AuthSession, SessionUser, UserRole } from "@/models/session";
import { getKeycloak, getKeycloakConfig } from "./keycloak";
import { loginRedirectUri, logoutRedirectUri } from "./redirects";
import { setLoginHandler, setTokenProvider } from "./session";
import { clearUserDrafts } from "@/lib/storage/user-drafts";

type AuthContextValue = AuthSession & {
  getToken: () => Promise<string | null>;
  login: (returnPath?: string | null) => Promise<void>;
  logout: () => Promise<void>;
  roles: UserRole[];
};

const AuthContext = createContext<AuthContextValue | null>(null);
let keycloakInitPromise: Promise<boolean> | null = null;
let loginRedirectInProgress = false;
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
  return role === "ADMIN" || role === "MUSEOLOGO" || role === "VIEWER";
}

function getRoles(token?: MuseoTokenParsed) {
  const realmRoles = token?.realm_access?.roles ?? [];
  const frontendRoles = token?.resource_access?.[getKeycloakConfig().clientId]?.roles ?? [];
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
  const keycloak = getKeycloak();
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
    const keycloak = getKeycloak();
    const nextAuthenticated = Boolean(keycloak.authenticated);
    setAuthenticated(nextAuthenticated);
    setUser(nextAuthenticated ? getUserFromToken(keycloak.tokenParsed as MuseoTokenParsed | undefined) : null);
  }, []);

  const login = useCallback(async (returnPath?: string | null) => {
    const keycloak = getKeycloak();
    if (keycloak.authenticated || loginRedirectInProgress) {
      return;
    }

    loginRedirectInProgress = true;
    window.sessionStorage.removeItem("museo.logout");
    await keycloak.login({
      redirectUri: loginRedirectUri(window.location.origin, returnPath)
    });
  }, []);

  const logout = useCallback(async () => {
    const keycloak = getKeycloak();
    loginRedirectInProgress = false;
    window.sessionStorage.setItem("museo.logout", "1");
    clearUserDrafts(user?.id);
    setAuthenticated(false);
    setUser(null);
    await keycloak.logout({
      redirectUri: logoutRedirectUri(window.location.origin)
    });
  }, [user?.id]);

  const getToken = useCallback(async () => {
    const keycloak = getKeycloak();
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
    const keycloak = getKeycloak();

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

        loginRedirectInProgress = false;
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
