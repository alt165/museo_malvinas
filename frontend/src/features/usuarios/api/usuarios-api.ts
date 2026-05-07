import { apiRequest } from "@/lib/api";
import type {
  AsignarRolRequestDTO,
  ResetPasswordRequestDTO,
  UsuarioKeycloakRequestDTO,
  UsuarioKeycloakResponseDTO
} from "../types";

const basePath = "/api/admin/usuarios";

export function listarUsuarios() {
  return apiRequest<UsuarioKeycloakResponseDTO[]>(basePath);
}

export function obtenerUsuarioPorId(id: string) {
  return apiRequest<UsuarioKeycloakResponseDTO>(`${basePath}/${id}`);
}

export function crearUsuario(payload: UsuarioKeycloakRequestDTO) {
  return apiRequest<UsuarioKeycloakResponseDTO>(basePath, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function actualizarUsuario(id: string, payload: UsuarioKeycloakRequestDTO) {
  return apiRequest<UsuarioKeycloakResponseDTO>(`${basePath}/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function cambiarEstadoUsuario(id: string, habilitado: boolean) {
  return apiRequest<UsuarioKeycloakResponseDTO>(`${basePath}/${id}/estado?habilitado=${String(habilitado)}`, {
    method: "PATCH"
  });
}

export function asignarRolUsuario(id: string, payload: AsignarRolRequestDTO) {
  return apiRequest<UsuarioKeycloakResponseDTO>(`${basePath}/${id}/roles`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function resetearPasswordUsuario(id: string, payload: ResetPasswordRequestDTO) {
  return apiRequest<void>(`${basePath}/${id}/reset-password`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}
