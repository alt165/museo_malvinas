"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { canWrite, hasRole } from "@/lib/auth";
import { useAuth } from "@/lib/auth/auth-provider";
import { requiresEditMode, routes } from "@/lib/routes";
import type { UserRole } from "@/models/session";
import { setEditingModeEnabled } from "./store";

type EditingModeContextValue = {
  canEdit: boolean;
  canEnableEditing: boolean;
  canAdminEdit: boolean;
  permitirEdicion: boolean;
  setPermitirEdicion: (enabled: boolean) => void;
};

type EditingModeStateProps = {
  authenticated: boolean;
  children: React.ReactNode;
  roles: UserRole[];
};

const EditingModeContext = createContext<EditingModeContextValue | null>(null);

function EditingModeState({ authenticated, children, roles }: EditingModeStateProps) {
  const pathname = usePathname();
  const router = useRouter();
  const [permitirEdicion, setPermitirEdicionState] = useState(false);
  const canEnableEditing = authenticated && canWrite(roles);
  const canEdit = canEnableEditing && permitirEdicion;
  const canAdminEdit = hasRole(roles, "ADMIN") && permitirEdicion;

  useEffect(() => {
    setEditingModeEnabled(canEdit);
  }, [canEdit]);

  useEffect(
    () => () => {
      setEditingModeEnabled(false);
    },
    []
  );

  const currentRouteRequiresEditing = requiresEditMode(pathname);

  useEffect(() => {
    if (!canEdit && currentRouteRequiresEditing) {
      router.replace(routes.dashboard);
    }
  }, [canEdit, currentRouteRequiresEditing, router]);

  const setPermitirEdicion = useCallback(
    (enabled: boolean) => {
      setPermitirEdicionState(canEnableEditing ? enabled : false);
    },
    [canEnableEditing]
  );

  const value = useMemo<EditingModeContextValue>(
    () => ({ canEdit, canEnableEditing, canAdminEdit, permitirEdicion: canEdit, setPermitirEdicion }),
    [canAdminEdit, canEdit, canEnableEditing, setPermitirEdicion]
  );

  return (
    <EditingModeContext.Provider value={value}>
      {!canEdit && currentRouteRequiresEditing ? null : children}
    </EditingModeContext.Provider>
  );
}

export function EditingModeProvider({ children }: { children: React.ReactNode }) {
  const { authenticated, roles, user } = useAuth();
  const editingSessionKey = authenticated ? user?.id ?? "authenticated" : "anonymous";

  return (
    <EditingModeState authenticated={authenticated} key={editingSessionKey} roles={roles}>
      {children}
    </EditingModeState>
  );
}

export function useEditingMode() {
  const context = useContext(EditingModeContext);

  if (!context) {
    throw new Error("useEditingMode must be used within EditingModeProvider");
  }

  return context;
}
