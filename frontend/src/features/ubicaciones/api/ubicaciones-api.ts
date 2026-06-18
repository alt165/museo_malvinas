import { apiRequest } from "@/lib/api";
import type { UbicacionRequestDTO, UbicacionResponseDTO } from "../types";

const basePath = "/api/ubicaciones";

export function listarUbicaciones() {
  return apiRequest<UbicacionResponseDTO[]>(basePath);
}

export function obtenerUbicacionPorId(id: number) {
  return apiRequest<UbicacionResponseDTO>(`${basePath}/${id}`);
}

export function crearUbicacion(payload: UbicacionRequestDTO) {
  return apiRequest<UbicacionResponseDTO>(basePath, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function actualizarUbicacion(id: number, payload: UbicacionRequestDTO) {
  return apiRequest<UbicacionResponseDTO>(`${basePath}/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function bajaLogicaUbicacion(id: number) {
  return apiRequest<void>(`${basePath}/${id}`, {
    method: "DELETE"
  });
}
