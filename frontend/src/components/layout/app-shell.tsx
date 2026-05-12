"use client";

import Image from "next/image";
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
      <div className="min-h-screen bg-background font-primary text-foreground">
        <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
        <div className="min-h-screen md:pl-64">
          <div className="sticky top-0 z-20">
            <div className="h-3 w-full overflow-hidden bg-primary">
              <Image
                alt=""
                aria-hidden="true"
                className="h-full w-full object-cover"
                height={12}
                priority
                src="/images/lieas-05.png"
                width={1440}
              />
            </div>
            <Topbar onOpenSidebar={() => setSidebarOpen(true)} />
          </div>
          <main className="min-h-[calc(100vh-4rem)] bg-background p-6">{children}</main>
        </div>
      </div>
    </ProtectedRoute>
  );
}
