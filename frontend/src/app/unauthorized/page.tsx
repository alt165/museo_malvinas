"use client";

import Link from "next/link";
import { useAuth } from "@/lib/auth";

export default function UnauthorizedPage() {
  const { logout, roles, user } = useAuth();

  return (
    <main className="flex min-h-screen items-center justify-center bg-background p-6">
      <div className="w-full max-w-md space-y-5 rounded-lg border bg-card p-6">
        <div className="space-y-2">
          <h1 className="text-xl font-semibold">Acceso no autorizado</h1>
          <p className="text-sm text-muted-foreground">
            El usuario autenticado no tiene permisos para acceder a esta seccion.
          </p>
        </div>
        <div className="rounded-md bg-muted p-3 text-sm">
          <p className="font-medium">{user?.name ?? user?.username ?? "Usuario"}</p>
          <p className="text-muted-foreground">{roles.join(", ") || "Sin roles"}</p>
        </div>
        <div className="flex gap-3">
          <Link
            className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm font-medium hover:bg-muted"
            href="/dashboard"
          >
            Volver
          </Link>
          <button
            className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90"
            onClick={() => void logout()}
            type="button"
          >
            Cerrar sesion
          </button>
        </div>
      </div>
    </main>
  );
}
