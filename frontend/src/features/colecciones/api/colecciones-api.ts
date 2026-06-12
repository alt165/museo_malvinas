import { apiBlobRequest, apiRequest } from "@/lib/api";
import type { ObjetoMuseoResponseDTO } from "@/features/objetos/types";
import type {
  AgregarObjetosColeccionRequestDTO,
  ColeccionObjetoRequestDTO,
  ColeccionObjetoResponseDTO
} from "../types";

const basePath = "/api/colecciones";

export function listarColecciones() {
  return apiRequest<ColeccionObjetoResponseDTO[]>(basePath);
}

export function obtenerColeccionPorId(id: number) {
  return apiRequest<ColeccionObjetoResponseDTO>(`${basePath}/${id}`);
}

export function crearColeccion(payload: ColeccionObjetoRequestDTO) {
  return apiRequest<ColeccionObjetoResponseDTO>(basePath, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function actualizarColeccion(id: number, payload: ColeccionObjetoRequestDTO) {
  return apiRequest<ColeccionObjetoResponseDTO>(`${basePath}/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function bajaLogicaColeccion(id: number) {
  return apiRequest<void>(`${basePath}/${id}`, {
    method: "DELETE"
  });
}

export function listarObjetosColeccion(id: number) {
  return apiRequest<ObjetoMuseoResponseDTO[]>(`${basePath}/${id}/objetos`);
}

export function exportarColeccionPdf(id: number) {
  return apiBlobRequest(`${basePath}/${id}/export/pdf`);
}

export function agregarObjetosColeccion(id: number, payload: AgregarObjetosColeccionRequestDTO) {
  return apiRequest<ObjetoMuseoResponseDTO[]>(`${basePath}/${id}/objetos`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function quitarObjetoColeccion(id: number, objetoId: number) {
  return apiRequest<void>(`${basePath}/${id}/objetos/${objetoId}`, {
    method: "DELETE"
  });
}
