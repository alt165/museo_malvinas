import { apiRequest } from "@/lib/api";
import type {
  ObjetoGrafoResponseDTO,
  RelacionObjetoPorObjetoResponseDTO,
  RelacionObjetoRequestDTO,
  RelacionObjetoResponseDTO
} from "../types";

const basePath = "/api/relaciones-objetos";

export function listarRelacionesObjeto() {
  return apiRequest<RelacionObjetoResponseDTO[]>(basePath);
}

export function obtenerRelacionObjetoPorId(id: number) {
  return apiRequest<RelacionObjetoResponseDTO>(`${basePath}/${id}`);
}

export function listarRelacionesDeObjeto(objetoId: number) {
  return apiRequest<RelacionObjetoPorObjetoResponseDTO[]>(`/api/objetos/${objetoId}/relaciones`);
}

export function obtenerGrafoRelacionesObjeto(objetoId: number, profundidad: number) {
  return apiRequest<ObjetoGrafoResponseDTO>(`/api/objetos/${objetoId}/grafo-relaciones?profundidad=${profundidad}`);
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
