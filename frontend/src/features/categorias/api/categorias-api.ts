import { apiRequest } from "@/lib/api";
import type { CategoriaObjetoRequestDTO, CategoriaObjetoResponseDTO } from "../types";
import { ordenarCategoriasPorNombre } from "../utils";

const basePath = "/api/categorias";

export async function listarCategorias() {
  const categorias = await apiRequest<CategoriaObjetoResponseDTO[]>(basePath);
  return ordenarCategoriasPorNombre(categorias);
}

export function obtenerCategoriaPorId(id: number) {
  return apiRequest<CategoriaObjetoResponseDTO>(`${basePath}/${id}`);
}

export function crearCategoria(payload: CategoriaObjetoRequestDTO) {
  return apiRequest<CategoriaObjetoResponseDTO>(basePath, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function actualizarCategoria(id: number, payload: CategoriaObjetoRequestDTO) {
  return apiRequest<CategoriaObjetoResponseDTO>(`${basePath}/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function bajaLogicaCategoria(id: number) {
  return apiRequest<void>(`${basePath}/${id}`, {
    method: "DELETE"
  });
}
