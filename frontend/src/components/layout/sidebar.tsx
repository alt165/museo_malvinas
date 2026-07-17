"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { ChevronDown, X } from "lucide-react";
import { useMemo, useState } from "react";
import { hasAnyRole, useAuth } from "@/lib/auth";
import { useEditingMode } from "@/lib/editing-mode";
import { navigationGroups, type NavigationGroup, type NavigationItem } from "@/lib/routes";
import { cn } from "@/lib/utils";
import type { UserRole } from "@/models/session";

type SidebarProps = {
  open: boolean;
  onClose: () => void;
};

function isRouteActive(pathname: string, href: string) {
  return pathname === href || pathname.startsWith(`${href}/`);
}

function activeHref(pathname: string, groups: NavigationGroup[]) {
  return groups
    .flatMap((group) => group.items)
    .filter((item) => !item.disabled)
    .filter((item) => isRouteActive(pathname, item.href))
    .sort((a, b) => b.href.length - a.href.length)[0]?.href;
}

function groupHasRoute(pathname: string, group: NavigationGroup) {
  return group.items.some((item) => !item.disabled && isRouteActive(pathname, item.href));
}

function visibleItems(items: NavigationItem[], roles: UserRole[], permitirEdicion: boolean) {
  return items.filter((item) => hasAnyRole(roles, item.roles) && (!item.requiresEditing || permitirEdicion));
}

export function Sidebar({ open, onClose }: SidebarProps) {
  const pathname = usePathname();
  const { roles } = useAuth();
  const { permitirEdicion } = useEditingMode();
  const visibleGroups = useMemo(
    () =>
      navigationGroups
        .filter((group) => hasAnyRole(roles, group.roles))
        .map((group) => ({ ...group, items: visibleItems(group.items, roles, permitirEdicion) }))
        .filter((group) => group.items.length > 0),
    [permitirEdicion, roles]
  );
  const currentActiveHref = activeHref(pathname, visibleGroups);
  const [expandedGroups, setExpandedGroups] = useState<string[]>([]);
  const [collapsedActiveGroups, setCollapsedActiveGroups] = useState<string[]>([]);

  function toggleGroup(key: string, expanded: boolean) {
    if (expanded) {
      setExpandedGroups((current) => current.filter((item) => item !== key));
      setCollapsedActiveGroups((current) => (current.includes(key) ? current : [...current, key]));
      return;
    }

    const activeGroupKeys = visibleGroups.filter((group) => groupHasRoute(pathname, group)).map((group) => group.key);

    setExpandedGroups([key]);
    setCollapsedActiveGroups(activeGroupKeys.filter((activeKey) => activeKey !== key));
  }

  return (
    <>
      <div
        className={cn("fixed inset-0 z-40 bg-black/30 md:hidden", open ? "block" : "hidden")}
        onClick={onClose}
      />
      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-50 flex w-72 flex-col border-r border-primary/15 bg-primary text-primary-foreground shadow-xl transition-transform md:z-30 md:w-64 md:translate-x-0",
          open ? "translate-x-0" : "-translate-x-full"
        )}
      >
        <div className="relative flex min-h-32 items-center justify-center border-b border-white/15 px-5 py-6">
          <Image
            alt="Museo Malvinas"
            className="h-auto max-h-24 w-44 object-contain"
            height={120}
            priority
            src="/images/logo-sidebar.png"
            width={220}
          />
          <button
            className="absolute right-3 top-3 inline-flex h-9 w-9 items-center justify-center rounded-md border border-white/20 text-white hover:bg-white/10 md:hidden"
            onClick={onClose}
            type="button"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
        <nav className="flex-1 overflow-y-auto p-3">
          <div className="space-y-2">
            {visibleGroups.map((group) => {
              const GroupIcon = group.icon;
              const groupActive = groupHasRoute(pathname, group);
              const expanded = expandedGroups.includes(group.key) || (groupActive && !collapsedActiveGroups.includes(group.key));

              return (
                <div className="space-y-1" key={group.key}>
                  <button
                    className={cn(
                      "flex w-full items-center gap-3 rounded-md border-l-2 border-transparent px-3 py-2.5 text-left text-sm transition-colors hover:bg-white/10",
                      groupActive ? "border-accent bg-white/10 text-white" : "text-white/75"
                    )}
                    onClick={() => toggleGroup(group.key, expanded)}
                    type="button"
                  >
                    <GroupIcon className="h-4 w-4 shrink-0" />
                    <span className="flex-1 font-medium">{group.label}</span>
                    <ChevronDown className={cn("h-4 w-4 transition-transform", expanded ? "rotate-180" : "rotate-0")} />
                  </button>
                  {expanded ? (
                    <div className="ml-5 space-y-1 border-l border-secondary/35 pl-3">
                      {group.items.map((item) => {
                        const active = currentActiveHref === item.href;
                        const ItemIcon = item.icon;

                        if (item.disabled) {
                          return (
                            <div
                              className="flex cursor-not-allowed items-center gap-2 rounded-md px-3 py-2 text-sm text-white/45"
                              key={`${group.key}-${item.label}`}
                            >
                              <ItemIcon className="h-3.5 w-3.5 shrink-0" />
                              <span className="flex-1">{item.label}</span>
                              {item.badge ? (
                                <span className="rounded border border-accent/60 px-1.5 py-0.5 text-[10px] uppercase tracking-wide text-accent">
                                  {item.badge}
                                </span>
                              ) : null}
                            </div>
                          );
                        }

                        return (
                          <Link
                            className={cn(
                              "flex items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors hover:bg-white/10",
                              active ? "bg-accent/20 font-medium text-white ring-1 ring-accent/45" : "text-white/70"
                            )}
                            href={item.href}
                            key={item.href}
                            onClick={onClose}
                          >
                            <ItemIcon className="h-3.5 w-3.5 shrink-0" />
                            <span>{item.label}</span>
                          </Link>
                        );
                      })}
                    </div>
                  ) : null}
                </div>
              );
            })}
          </div>
        </nav>
      </aside>
    </>
  );
}
