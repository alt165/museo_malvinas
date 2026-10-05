import { apiBlobRequest, apiRequest } from "@/lib/api";
import type { ObjetoMuseoResponseDTO, PageResponse } from "@/features/objetos/types";
import type { DepositanteRequestDTO, DepositanteResponseDTO } from "../types";

const basePath = "/api/depositantes";

export function listarDepositantes() {
  return apiRequest<DepositanteResponseDTO[]>(basePath);
}

export type BuscarDepositantesParams = {
  texto?: string;
  page?: number;
  size?: number;
};

export function buscarDepositantes(params: BuscarDepositantesParams) {
  const searchParams = new URLSearchParams();
  if (params.texto?.trim()) searchParams.set("texto", params.texto.trim());
  searchParams.set("page", String(params.page ?? 0));
  searchParams.set("size", String(params.size ?? 20));
  searchParams.set("sort", "nombre,asc");
  return apiRequest<PageResponse<DepositanteResponseDTO>>(basePath + "/buscar?" + searchParams.toString());
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

export function listarObjetosDepositante(id: number) {
  return apiRequest<ObjetoMuseoResponseDTO[]>(`${basePath}/${id}/objetos`);
}

export function exportarObjetosDepositantePdf(id: number) {
  return apiBlobRequest(`${basePath}/${id}/objetos/export/pdf`);
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
