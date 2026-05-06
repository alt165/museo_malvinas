"use client";

import { useState } from "react";
import { Sidebar } from "./sidebar";
import { Topbar } from "./topbar";
import { ProtectedRoute } from "@/lib/auth";
import type { UserRole } from "@/models/session";

type AppShellProps = {
  children: React.ReactNode;
  requiredRoles?: UserRole[];
};

export function AppShell({ children, requiredRoles }: AppShellProps) {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <ProtectedRoute requiredRoles={requiredRoles}>
      <div className="min-h-screen bg-background">
        <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
        <div className="min-h-screen md:pl-64">
          <Topbar onOpenSidebar={() => setSidebarOpen(true)} />
          <main className="p-6">{children}</main>
        </div>
      </div>
    </ProtectedRoute>
  );
}
