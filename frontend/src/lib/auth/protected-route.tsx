"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import type { UserRole } from "@/models/session";
import { hasAnyRole } from "./permissions";
import { useAuth } from "./auth-provider";

type ProtectedRouteProps = {
  children: React.ReactNode;
  requiredRoles?: UserRole[];
};

export function ProtectedRoute({ children, requiredRoles }: ProtectedRouteProps) {
  const router = useRouter();
  const { authenticated, loading, login, roles } = useAuth();

  const allowed = !requiredRoles || hasAnyRole(roles, requiredRoles);

  useEffect(() => {
    if (loading) {
      return;
    }

    if (!authenticated) {
      void login();
      return;
    }

    if (!allowed) {
      router.replace("/unauthorized");
    }
  }, [allowed, authenticated, loading, login, router]);

  if (loading || !authenticated) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background text-sm text-muted-foreground">
        Cargando sesion...
      </div>
    );
  }

  if (!allowed) {
    return null;
  }

  return children;
}
