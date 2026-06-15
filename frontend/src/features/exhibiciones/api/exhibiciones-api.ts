import { apiRequest } from "@/lib/api";
import type { PageResponse } from "@/features/objetos/types";
import type { ExhibicionObjetoRequestDTO, ExhibicionObjetoResponseDTO, ExhibicionRequestDTO, ExhibicionResponseDTO, ObjetoDisponibilidadExhibicionResponseDTO } from "../types";

const exhibicionesPath = "/api/exhibiciones";
const exhibicionesObjetosPath = "/api/exhibiciones-objetos";

export function listarExhibiciones() {
  return apiRequest<ExhibicionResponseDTO[]>(exhibicionesPath);
}

export function obtenerExhibicionPorId(id: number) {
  return apiRequest<ExhibicionResponseDTO>(`${exhibicionesPath}/${id}`);
}

export function crearExhibicion(payload: ExhibicionRequestDTO) {
  return apiRequest<ExhibicionResponseDTO>(exhibicionesPath, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function actualizarExhibicion(id: number, payload: ExhibicionRequestDTO) {
  return apiRequest<ExhibicionResponseDTO>(`${exhibicionesPath}/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export type BuscarDisponibilidadExhibicionParams = {
  texto?: string;
  fechaInicio: string;
  fechaFin?: string | null;
  exhibicionId?: number;
  page?: number;
  size?: number;
};

export function buscarObjetosDisponibilidadExhibicion(params: BuscarDisponibilidadExhibicionParams) {
  const searchParams = new URLSearchParams();
  if (params.texto?.trim()) searchParams.set("texto", params.texto.trim());
  searchParams.set("fechaInicio", params.fechaInicio);
  if (params.fechaFin) searchParams.set("fechaFin", params.fechaFin);
  if (params.exhibicionId) searchParams.set("exhibicionId", String(params.exhibicionId));
  searchParams.set("page", String(params.page ?? 0));
  searchParams.set("size", String(params.size ?? 10));
  searchParams.set("sort", "numeroInventario,asc");
  return apiRequest<PageResponse<ObjetoDisponibilidadExhibicionResponseDTO>>(`${exhibicionesPath}/objetos-disponibilidad?${searchParams.toString()}`);
}

export type BuscarExhibicionesFinalizadasParams = {
  texto?: string;
  page?: number;
  size?: number;
};

export function buscarExhibicionesFinalizadas(params: BuscarExhibicionesFinalizadasParams) {
  const searchParams = new URLSearchParams();
  if (params.texto?.trim()) searchParams.set("texto", params.texto.trim());
  searchParams.set("page", String(params.page ?? 0));
  searchParams.set("size", String(params.size ?? 10));
  searchParams.set("sort", "nombre,asc");
  return apiRequest<PageResponse<ExhibicionResponseDTO>>(`${exhibicionesPath}/finalizadas/buscar?${searchParams.toString()}`);
}

export type ObjetosParaRepetirParams = {
  exhibicionId: number;
  fechaInicioNueva: string;
  fechaFinNueva?: string | null;
};

export function obtenerObjetosParaRepetirExhibicion(params: ObjetosParaRepetirParams) {
  const searchParams = new URLSearchParams();
  searchParams.set("fechaInicioNueva", params.fechaInicioNueva);
  if (params.fechaFinNueva) searchParams.set("fechaFinNueva", params.fechaFinNueva);
  return apiRequest<ObjetoDisponibilidadExhibicionResponseDTO[]>(`${exhibicionesPath}/${params.exhibicionId}/objetos-para-repetir?${searchParams.toString()}`);
}

export function bajaLogicaExhibicion(id: number) {
  return apiRequest<void>(`${exhibicionesPath}/${id}`, {
    method: "DELETE"
  });
}

export function finalizarExhibicion(id: number) {
  return apiRequest<ExhibicionResponseDTO>(`${exhibicionesPath}/${id}/finalizar`, {
    method: "POST"
  });
}

export function listarObjetosDeExhibicion() {
  return apiRequest<ExhibicionObjetoResponseDTO[]>(exhibicionesObjetosPath);
}

export function agregarObjetoAExhibicion(payload: ExhibicionObjetoRequestDTO) {
  return apiRequest<ExhibicionObjetoResponseDTO>(exhibicionesObjetosPath, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function actualizarObjetoDeExhibicion(id: number, payload: ExhibicionObjetoRequestDTO) {
  return apiRequest<ExhibicionObjetoResponseDTO>(`${exhibicionesObjetosPath}/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function verificarDevolucionObjeto(id: number, observaciones?: string) {
  const params = new URLSearchParams();

  if (observaciones) {
    params.set("observaciones", observaciones);
  }

  const suffix = params.size > 0 ? `?${params.toString()}` : "";

  return apiRequest<ExhibicionObjetoResponseDTO>(`${exhibicionesObjetosPath}/${id}/verificar-devolucion${suffix}`, {
    method: "POST"
  });
}

export function revertirDevolucionObjeto(id: number) {
  return apiRequest<ExhibicionObjetoResponseDTO>(`${exhibicionesObjetosPath}/${id}/revertir-devolucion`, {
    method: "POST"
  });
}
