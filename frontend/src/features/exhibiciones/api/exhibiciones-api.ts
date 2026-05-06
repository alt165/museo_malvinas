import { apiRequest } from "@/lib/api";
import type { ExhibicionObjetoRequestDTO, ExhibicionObjetoResponseDTO, ExhibicionRequestDTO, ExhibicionResponseDTO } from "../types";

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
