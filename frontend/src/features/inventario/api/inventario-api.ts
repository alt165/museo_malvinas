import { apiRequest } from "@/lib/api";
import type { InventarioRequestDTO, InventarioResponseDTO, MovimientoInventarioResponseDTO } from "../types";

const inventariosPath = "/api/inventarios";
const movimientosPath = "/api/movimientos-inventario";

export function listarInventarios() {
  return apiRequest<InventarioResponseDTO[]>(inventariosPath);
}

export function obtenerInventarioPorId(id: number) {
  return apiRequest<InventarioResponseDTO>(`${inventariosPath}/${id}`);
}

export function crearInventario(payload: InventarioRequestDTO) {
  return apiRequest<InventarioResponseDTO>(inventariosPath, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function actualizarInventario(id: number, payload: InventarioRequestDTO) {
  return apiRequest<InventarioResponseDTO>(`${inventariosPath}/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function bajaLogicaInventario(id: number) {
  return apiRequest<void>(`${inventariosPath}/${id}`, {
    method: "DELETE"
  });
}

export function listarMovimientosInventario() {
  return apiRequest<MovimientoInventarioResponseDTO[]>(movimientosPath);
}

export function obtenerMovimientoPorId(id: number) {
  return apiRequest<MovimientoInventarioResponseDTO>(`${movimientosPath}/${id}`);
}
