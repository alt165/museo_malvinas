"use client";

import Link from "next/link";
import { hasAnyRole, useAuth } from "@/lib/auth";
import type { UserRole } from "@/models/session";

type PermissionLinkProps = {
  href: string;
  label: string;
  roles?: UserRole[];
};

export function PermissionLink({ href, label, roles }: PermissionLinkProps) {
  const { roles: userRoles } = useAuth();

  if (roles && !hasAnyRole(userRoles, roles)) {
    return null;
  }

  return (
    <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={href}>
      {label}
    </Link>
  );
}
