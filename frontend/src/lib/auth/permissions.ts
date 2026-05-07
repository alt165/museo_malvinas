import type { UserRole } from "@/models/session";

const writeRoles: UserRole[] = ["ADMIN", "OPERATOR"];
const readRoles: UserRole[] = ["ADMIN", "OPERATOR", "VIEWER"];
const adminRoles: UserRole[] = ["ADMIN"];

export function hasRole(roles: UserRole[], role: UserRole) {
  return roles.includes(role);
}

export function hasAnyRole(roles: UserRole[], allowedRoles: UserRole[]) {
  return allowedRoles.some((role) => hasRole(roles, role));
}

export function canRead(roles: UserRole[]) {
  return hasAnyRole(roles, readRoles);
}

export function canWrite(roles: UserRole[]) {
  return hasAnyRole(roles, writeRoles);
}

export const permissions = {
  adminRoles,
  readRoles,
  writeRoles
} as const;
