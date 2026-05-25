"use client";

import { LogOut, Menu } from "lucide-react";
import { useAuth } from "@/lib/auth";

type TopbarProps = {
  onOpenSidebar: () => void;
};

export function Topbar({ onOpenSidebar }: TopbarProps) {
  const { logout, user } = useAuth();
  const displayName = user?.name ?? user?.username ?? "Usuario";

  return (
    <header className="relative z-10 flex h-full items-center justify-between px-4 text-white md:px-6">
      <button
        className="inline-flex h-9 w-9 items-center justify-center rounded-md border border-white/30 bg-white/10 text-white hover:bg-white/20 md:hidden"
        onClick={onOpenSidebar}
        type="button"
      >
        <Menu className="h-4 w-4" />
      </button>
      <div className="ml-auto flex min-w-0 items-center gap-3">
        <p className="min-w-0 truncate text-sm font-semibold text-white">{displayName}</p>
        <button
          className="inline-flex h-9 items-center gap-2 rounded-md border border-white/30 bg-white/10 px-3 text-sm font-medium text-white hover:bg-white/20"
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
