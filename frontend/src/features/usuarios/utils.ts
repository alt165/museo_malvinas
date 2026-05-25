import { ApiClientError, getUserFacingErrorMessage } from "@/lib/errors/api-error";
import type { UserRole } from "@/models/session";
import type { UsuarioFormValues } from "./schemas";
import type { UsuarioKeycloakRequestDTO, UsuarioKeycloakResponseDTO } from "./types";

export function getApiErrorMessage(error: unknown) {
  return getUserFacingErrorMessage(error);
}

export function getValidationErrors(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.payload?.validationErrors ?? {};
  }

  return {};
}

export function usuarioToFormValues(usuario?: UsuarioKeycloakResponseDTO): UsuarioFormValues {
  return {
    username: usuario?.username ?? "",
    email: usuario?.email ?? "",
    dni: usuario?.dni ?? "",
    nombre: usuario?.nombre ?? "",
    apellido: usuario?.apellido ?? "",
    habilitado: usuario?.habilitado ?? true,
    rol: getRolPrincipal(usuario?.roles),
    contrasenaInicial: ""
  };
}

export function formValuesToUsuarioRequest(values: UsuarioFormValues): UsuarioKeycloakRequestDTO {
  const contrasenaInicial = values.contrasenaInicial?.trim();

  return {
    username: values.username.trim(),
    email: values.email.trim(),
    dni: values.dni.trim(),
    nombre: normalizeOptional(values.nombre),
    apellido: normalizeOptional(values.apellido),
    habilitado: values.habilitado,
    contrasenaInicial: contrasenaInicial || null,
    roles: [values.rol]
  };
}

export function getRolPrincipal(roles?: UserRole[]): UserRole {
  if (roles?.includes("ADMIN")) {
    return "ADMIN";
  }

  if (roles?.includes("OPERATOR")) {
    return "OPERATOR";
  }

  return "VIEWER";
}

export function nombreCompleto(usuario: UsuarioKeycloakResponseDTO) {
  return [usuario.nombre, usuario.apellido].filter(Boolean).join(" ") || "Sin nombre";
}

function normalizeOptional(value?: string | null) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : null;
}
