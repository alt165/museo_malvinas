import { apiRequest } from "@/lib/api";
import type { RelacionObjetoRequestDTO, RelacionObjetoResponseDTO } from "../types";

const basePath = "/api/relaciones-objetos";

export function listarRelacionesObjeto() {
  return apiRequest<RelacionObjetoResponseDTO[]>(basePath);
}

export function obtenerRelacionObjetoPorId(id: number) {
  return apiRequest<RelacionObjetoResponseDTO>(`${basePath}/${id}`);
}

export async function listarRelacionesPorObjeto(objetoId: number) {
  const relaciones = await listarRelacionesObjeto();
  return relaciones.filter(
    (relacion) => relacion.objetoOrigenId === objetoId || relacion.objetoDestinoId === objetoId
  );
}

export function crearRelacionObjeto(payload: RelacionObjetoRequestDTO) {
  return apiRequest<RelacionObjetoResponseDTO>(basePath, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function actualizarRelacionObjeto(id: number, payload: RelacionObjetoRequestDTO) {
  return apiRequest<RelacionObjetoResponseDTO>(`${basePath}/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function bajaLogicaRelacionObjeto(id: number) {
  return apiRequest<void>(`${basePath}/${id}`, {
    method: "DELETE"
  });
}
