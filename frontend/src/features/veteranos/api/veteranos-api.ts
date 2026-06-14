import { apiRequest } from "@/lib/api";
import type {
  ActuacionVeteranoRequestDTO,
  ActuacionVeteranoResponseDTO,
  Fuerza,
  ObjetoVeteranoRequestDTO,
  ObjetoVeteranoResponseDTO,
  RangoMilitarResponseDTO,
  UnidadMilitarResponseDTO,
  VeteranoRequestDTO,
  VeteranoResponseDTO
} from "../types";

const veteranosPath = "/api/veteranos";
const actuacionesPath = "/api/actuaciones-veteranos";
const objetosVeteranosPath = "/api/objetos-veteranos";
const rangosMilitaresPath = "/api/rangos-militares";
const unidadesMilitaresPath = "/api/unidades-militares";

export function listarVeteranos() {
  return apiRequest<VeteranoResponseDTO[]>(veteranosPath);
}

export function obtenerVeteranoPorId(id: number) {
  return apiRequest<VeteranoResponseDTO>(`${veteranosPath}/${id}`);
}

export function crearVeterano(payload: VeteranoRequestDTO) {
  return apiRequest<VeteranoResponseDTO>(veteranosPath, { method: "POST", body: JSON.stringify(payload) });
}

export function actualizarVeterano(id: number, payload: VeteranoRequestDTO) {
  return apiRequest<VeteranoResponseDTO>(`${veteranosPath}/${id}`, { method: "PUT", body: JSON.stringify(payload) });
}

export function bajaLogicaVeterano(id: number) {
  return apiRequest<void>(`${veteranosPath}/${id}`, { method: "DELETE" });
}

export function listarActuacionesVeteranos() {
  return apiRequest<ActuacionVeteranoResponseDTO[]>(actuacionesPath);
}

export function obtenerActuacionVeteranoPorId(id: number) {
  return apiRequest<ActuacionVeteranoResponseDTO>(`${actuacionesPath}/${id}`);
}

export async function listarActuacionesPorVeterano(veteranoId: number) {
  const actuaciones = await listarActuacionesVeteranos();
  return actuaciones.filter((actuacion) => actuacion.veteranoId === veteranoId);
}

export function crearActuacionVeterano(payload: ActuacionVeteranoRequestDTO) {
  return apiRequest<ActuacionVeteranoResponseDTO>(actuacionesPath, { method: "POST", body: JSON.stringify(payload) });
}

export function actualizarActuacionVeterano(id: number, payload: ActuacionVeteranoRequestDTO) {
  return apiRequest<ActuacionVeteranoResponseDTO>(`${actuacionesPath}/${id}`, { method: "PUT", body: JSON.stringify(payload) });
}

export function bajaLogicaActuacionVeterano(id: number) {
  return apiRequest<void>(`${actuacionesPath}/${id}`, { method: "DELETE" });
}

export function listarObjetosRelacionadosAVeterano(veteranoId: number) {
  return apiRequest<ObjetoVeteranoResponseDTO[]>(`${objetosVeteranosPath}?veteranoId=${veteranoId}`);
}

export function listarRelacionesObjetoVeterano() {
  return apiRequest<ObjetoVeteranoResponseDTO[]>(objetosVeteranosPath);
}

export function obtenerRelacionObjetoVeteranoPorId(id: number) {
  return apiRequest<ObjetoVeteranoResponseDTO>(`${objetosVeteranosPath}/${id}`);
}

export function asociarObjetoAVeterano(payload: ObjetoVeteranoRequestDTO) {
  return apiRequest<ObjetoVeteranoResponseDTO>(objetosVeteranosPath, { method: "POST", body: JSON.stringify(payload) });
}

export function eliminarRelacionObjetoVeterano(id: number) {
  return apiRequest<void>(`${objetosVeteranosPath}/${id}`, { method: "DELETE" });
}

export function listarRangosMilitares(fuerza: Fuerza) {
  return apiRequest<RangoMilitarResponseDTO[]>(`${rangosMilitaresPath}?fuerza=${encodeURIComponent(fuerza)}`);
}

export function buscarUnidadesMilitares(fuerza: Fuerza, buscar = "", limite = 20) {
  const params = new URLSearchParams({ fuerza, limite: String(limite) });
  const filtro = buscar.trim();
  if (filtro) {
    params.set("buscar", filtro);
  }
  return apiRequest<UnidadMilitarResponseDTO[]>(`${unidadesMilitaresPath}?${params.toString()}`);
}
