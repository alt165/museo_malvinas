"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { X } from "lucide-react";
import { hasAnyRole, useAuth } from "@/lib/auth";
import { navigationItems, operationActions } from "@/lib/routes";
import { cn } from "@/lib/utils";

type SidebarProps = {
  open: boolean;
  onClose: () => void;
};

export function Sidebar({ open, onClose }: SidebarProps) {
  const pathname = usePathname();
  const { roles } = useAuth();
  const visibleItems = navigationItems.filter((item) => hasAnyRole(roles, item.roles));
  const visibleActions = operationActions.filter((item) => hasAnyRole(roles, item.roles));

  return (
    <>
      <div
        className={cn("fixed inset-0 z-40 bg-black/30 md:hidden", open ? "block" : "hidden")}
        onClick={onClose}
      />
      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-50 flex w-72 flex-col border-r bg-card transition-transform md:z-30 md:w-64 md:translate-x-0",
          open ? "translate-x-0" : "-translate-x-full"
        )}
      >
        <div className="flex h-14 items-center justify-between border-b px-5">
          <div>
            <p className="text-sm font-semibold">Museo Malvinas</p>
            <p className="text-xs text-muted-foreground">Administracion</p>
          </div>
          <button
            className="inline-flex h-9 w-9 items-center justify-center rounded-md border md:hidden"
            onClick={onClose}
            type="button"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
        <nav className="flex-1 space-y-6 overflow-y-auto p-3">
          <div className="space-y-1">
            {visibleItems.map((item) => {
              const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
              const Icon = item.icon;

              return (
                <Link
                  className={cn(
                    "flex items-start gap-3 rounded-md px-3 py-2.5 text-sm transition-colors hover:bg-muted hover:text-foreground",
                    active ? "bg-muted text-foreground" : "text-muted-foreground"
                  )}
                  href={item.href}
                  key={item.href}
                  onClick={onClose}
                >
                  <Icon className="mt-0.5 h-4 w-4 shrink-0" />
                  <span>
                    <span className="block font-medium">{item.label}</span>
                    <span className="block text-xs text-muted-foreground">{item.description}</span>
                  </span>
                </Link>
              );
            })}
          </div>
          {visibleActions.length > 0 ? (
            <div className="space-y-2 border-t pt-4">
              <p className="px-3 text-xs font-medium uppercase text-muted-foreground">Acciones</p>
              {visibleActions.map((item) => (
                <Link
                  className="block rounded-md border px-3 py-2 text-sm font-medium hover:bg-muted"
                  href={item.href}
                  key={item.href}
                  onClick={onClose}
                >
                  {item.label}
                </Link>
              ))}
            </div>
          ) : null}
        </nav>
      </aside>
    </>
  );
}
