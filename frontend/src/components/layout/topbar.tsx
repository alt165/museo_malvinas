"use client";

import { LogOut } from "lucide-react";
import { useAuth } from "@/lib/auth";

export function Topbar() {
  const { logout, roles, user } = useAuth();
  const displayName = user?.name ?? user?.username ?? "Usuario";

  return (
    <header className="flex h-14 items-center justify-between border-b bg-background px-6">
      <div className="text-sm font-medium">Administracion</div>
      <div className="flex items-center gap-4">
        <div className="text-right">
          <p className="text-sm font-medium">{displayName}</p>
          <p className="text-xs text-muted-foreground">{roles.join(", ") || "Sin roles"}</p>
        </div>
        <button
          className="inline-flex h-9 items-center gap-2 rounded-md border px-3 text-sm hover:bg-muted"
          onClick={() => void logout()}
          type="button"
        >
          <LogOut className="h-4 w-4" />
          Salir
        </button>
      </div>
    </header>
  );
}
