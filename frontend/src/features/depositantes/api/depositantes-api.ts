import { apiRequest } from "@/lib/api";
import type { DepositanteRequestDTO, DepositanteResponseDTO } from "../types";

const basePath = "/api/depositantes";

export function listarDepositantes() {
  return apiRequest<DepositanteResponseDTO[]>(basePath);
}

export function obtenerDepositantePorId(id: number) {
  return apiRequest<DepositanteResponseDTO>(`${basePath}/${id}`);
}

export function buscarDepositantePorIdentificacion(valor: string) {
  return apiRequest<DepositanteResponseDTO>(`${basePath}/buscar-identificacion?valor=${encodeURIComponent(valor)}`);
}

export function buscarDepositantesPorNombre(valor: string) {
  return apiRequest<DepositanteResponseDTO[]>(`${basePath}/buscar-nombre?valor=${encodeURIComponent(valor)}`);
}

export function crearDepositante(payload: DepositanteRequestDTO) {
  return apiRequest<DepositanteResponseDTO>(basePath, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function actualizarDepositante(id: number, payload: DepositanteRequestDTO) {
  return apiRequest<DepositanteResponseDTO>(`${basePath}/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function bajaLogicaDepositante(id: number) {
  return apiRequest<void>(`${basePath}/${id}`, {
    method: "DELETE"
  });
}
