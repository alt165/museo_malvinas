import { apiRequest } from "@/lib/api";
import type { ObjetoMuseoRequestDTO, ObjetoMuseoResponseDTO } from "../types";

const basePath = "/api/objetos";

export function listarObjetos() {
  return apiRequest<ObjetoMuseoResponseDTO[]>(basePath);
}

export function obtenerObjetoPorId(id: number) {
  return apiRequest<ObjetoMuseoResponseDTO>(`${basePath}/${id}`);
}

export function crearObjeto(payload: ObjetoMuseoRequestDTO) {
  return apiRequest<ObjetoMuseoResponseDTO>(basePath, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function actualizarObjeto(id: number, payload: ObjetoMuseoRequestDTO) {
  return apiRequest<ObjetoMuseoResponseDTO>(`${basePath}/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function bajaLogicaObjeto(id: number) {
  return apiRequest<void>(`${basePath}/${id}`, {
    method: "DELETE"
  });
}
