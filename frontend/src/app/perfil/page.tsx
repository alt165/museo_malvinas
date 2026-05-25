"use client";

import { AppShell } from "@/components/layout/app-shell";
import { PageHeader } from "@/components/common/page-header";
import { useAuth } from "@/lib/auth";
import { formatRoles } from "@/lib/auth/role-labels";

export default function PerfilPage() {
  const { authenticated, roles, user } = useAuth();

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          description="Informacion de la sesion autenticada en Keycloak."
          title="Perfil"
        />
        <div className="rounded-lg border p-5">
          <dl className="grid gap-4 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-muted-foreground">Estado</dt>
              <dd className="mt-1 font-medium">{authenticated ? "Sesion activa" : "Sin sesion"}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Usuario</dt>
              <dd className="mt-1 font-medium">{user?.username ?? "No disponible"}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Nombre</dt>
              <dd className="mt-1 font-medium">{user?.name ?? "No disponible"}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Email</dt>
              <dd className="mt-1 font-medium">{user?.email ?? "No disponible"}</dd>
            </div>
            <div className="sm:col-span-2">
              <dt className="text-muted-foreground">Roles</dt>
              <dd className="mt-1 font-medium">{formatRoles(roles, "Sin roles")}</dd>
            </div>
          </dl>
        </div>
      </div>
    </AppShell>
  );
}
