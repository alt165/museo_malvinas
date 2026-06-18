import type { UserRole } from "@/models/session";

export const rolesUsuario: UserRole[] = ["ADMIN", "OPERATOR", "VIEWER"];

export type UsuarioKeycloakRequestDTO = {
  username: string;
  email: string;
  dni: string;
  nombre?: string | null;
  apellido?: string | null;
  habilitado?: boolean;
  contrasenaInicial?: string | null;
  roles?: UserRole[];
};

export type UsuarioKeycloakResponseDTO = {
  id: string;
  username: string;
  email: string;
  dni: string;
  nombre?: string | null;
  apellido?: string | null;
  habilitado: boolean;
  roles: UserRole[];
};

export type AsignarRolRequestDTO = {
  roles: UserRole[];
  confirmarQuitarAdminPropio?: boolean;
};

export type ResetPasswordRequestDTO = {
  contrasena: string;
};
