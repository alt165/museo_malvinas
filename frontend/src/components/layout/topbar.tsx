"use client";

import { LogOut, Menu } from "lucide-react";
import { useAuth } from "@/lib/auth";

type TopbarProps = {
  onOpenSidebar: () => void;
};

export function Topbar({ onOpenSidebar }: TopbarProps) {
  const { authenticated, logout, roles, user } = useAuth();
  const displayName = user?.name ?? user?.username ?? "Usuario";

  return (
    <header className="sticky top-0 z-20 flex h-14 items-center justify-between border-b bg-background px-4 md:px-6">
      <div className="flex items-center gap-3">
        <button
          className="inline-flex h-9 w-9 items-center justify-center rounded-md border md:hidden"
          onClick={onOpenSidebar}
          type="button"
        >
          <Menu className="h-4 w-4" />
        </button>
        <div>
          <div className="text-sm font-medium">Administracion</div>
          <div className="text-xs text-muted-foreground">
            {authenticated ? "Sesion activa" : "Sesion no autenticada"}
          </div>
        </div>
      </div>
      <div className="flex min-w-0 items-center gap-3">
        <div className="hidden min-w-0 text-right sm:block">
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
