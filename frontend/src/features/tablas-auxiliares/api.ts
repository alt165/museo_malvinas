import { apiRequest } from "@/lib/api";
import type {
  DetalleConservacionRequestDTO,
  DetalleConservacionResponseDTO,
  RangoMilitarRequestDTO,
  RangoMilitarResponseDTO,
  UnidadMilitarRequestDTO,
  UnidadMilitarResponseDTO
} from "./types";

const rangosPath = "/api/rangos-militares";
const unidadesPath = "/api/unidades-militares";
const detallesPath = "/api/detalles-conservacion";

export async function listarRangosMilitaresAdmin() {
  const rangos = await apiRequest<RangoMilitarResponseDTO[]>(rangosPath);
  return [...rangos].sort((a, b) => a.fuerza.localeCompare(b.fuerza) || a.ordenJerarquico - b.ordenJerarquico || a.nombre.localeCompare(b.nombre, "es", { sensitivity: "base" }));
}

export function crearRangoMilitar(payload: RangoMilitarRequestDTO) {
  return apiRequest<RangoMilitarResponseDTO>(rangosPath, { method: "POST", body: JSON.stringify(payload) });
}

export function actualizarRangoMilitar(id: number, payload: RangoMilitarRequestDTO) {
  return apiRequest<RangoMilitarResponseDTO>(`${rangosPath}/${id}`, { method: "PUT", body: JSON.stringify(payload) });
}

export function bajaLogicaRangoMilitar(id: number) {
  return apiRequest<void>(`${rangosPath}/${id}`, { method: "DELETE" });
}

export async function listarUnidadesMilitaresAdmin() {
  const unidades = await apiRequest<UnidadMilitarResponseDTO[]>(unidadesPath);
  return [...unidades].sort((a, b) => a.fuerza.localeCompare(b.fuerza) || a.nombre.localeCompare(b.nombre, "es", { sensitivity: "base" }));
}

export function crearUnidadMilitar(payload: UnidadMilitarRequestDTO) {
  return apiRequest<UnidadMilitarResponseDTO>(unidadesPath, { method: "POST", body: JSON.stringify(payload) });
}

export function actualizarUnidadMilitar(id: number, payload: UnidadMilitarRequestDTO) {
  return apiRequest<UnidadMilitarResponseDTO>(`${unidadesPath}/${id}`, { method: "PUT", body: JSON.stringify(payload) });
}

export function bajaLogicaUnidadMilitar(id: number) {
  return apiRequest<void>(`${unidadesPath}/${id}`, { method: "DELETE" });
}

export async function listarDetallesConservacion() {
  const detalles = await apiRequest<DetalleConservacionResponseDTO[]>(detallesPath);
  return [...detalles].sort((a, b) => a.nombre.localeCompare(b.nombre, "es", { sensitivity: "base" }));
}

export function crearDetalleConservacion(payload: DetalleConservacionRequestDTO) {
  return apiRequest<DetalleConservacionResponseDTO>(detallesPath, { method: "POST", body: JSON.stringify(payload) });
}

export function actualizarDetalleConservacion(id: number, payload: DetalleConservacionRequestDTO) {
  return apiRequest<DetalleConservacionResponseDTO>(`${detallesPath}/${id}`, { method: "PUT", body: JSON.stringify(payload) });
}

export function bajaLogicaDetalleConservacion(id: number) {
  return apiRequest<void>(`${detallesPath}/${id}`, { method: "DELETE" });
}
