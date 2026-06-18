import type { UserRole } from "@/models/session";

const roleLabels: Record<UserRole, string> = {
  ADMIN: "Administrador",
  OPERATOR: "Museólogo",
  VIEWER: "Consulta"
};

export function getRoleLabel(role: UserRole) {
  return roleLabels[role] ?? role;
}

export function formatRoles(roles?: UserRole[], emptyLabel = "Sin rol") {
  return roles && roles.length > 0 ? roles.map(getRoleLabel).join(", ") : emptyLabel;
}
