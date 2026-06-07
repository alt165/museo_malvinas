"use client";

import Image from "next/image";
import { useState } from "react";
import { Sidebar } from "./sidebar";
import { Topbar } from "./topbar";
import { ProtectedRoute } from "@/lib/auth";
import { useEditingMode } from "@/lib/editing-mode";
import type { UserRole } from "@/models/session";

type AppShellProps = {
  children: React.ReactNode;
  requiredRoles?: UserRole[];
};

export function AppShell({ children, requiredRoles }: AppShellProps) {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { permitirEdicion } = useEditingMode();

  return (
    <ProtectedRoute requiredRoles={requiredRoles}>
      <div className="min-h-screen bg-background font-primary text-foreground">
        <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
        <div className="min-h-screen md:pl-64">
          <div className="sticky top-0 z-20">
            <div className="relative h-[68px] overflow-hidden bg-primary">
              <Image
                alt=""
                aria-hidden="true"
                className="absolute inset-0 h-full w-full object-cover"
                height={68}
                priority
                src="/images/lieas-05.png"
                width={1440}
              />
              <Topbar onOpenSidebar={() => setSidebarOpen(true)} />
            </div>
          </div>
          <main className="min-h-[calc(100vh-68px)] bg-background p-6">
            {permitirEdicion ? (
              <div className="mb-6 rounded-md border border-destructive bg-destructive/10 px-4 py-3 text-sm font-semibold text-destructive">
                Modo edición activado: es posible modificar datos del sistema.
              </div>
            ) : null}
            {children}
          </main>
        </div>
      </div>
    </ProtectedRoute>
  );
}
