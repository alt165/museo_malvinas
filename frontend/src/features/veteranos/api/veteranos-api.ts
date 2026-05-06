import { apiRequest } from "@/lib/api";
import type {
  ActuacionVeteranoRequestDTO,
  ActuacionVeteranoResponseDTO,
  ObjetoVeteranoRequestDTO,
  ObjetoVeteranoResponseDTO,
  VeteranoRequestDTO,
  VeteranoResponseDTO
} from "../types";

const veteranosPath = "/api/veteranos";
const actuacionesPath = "/api/actuaciones-veteranos";
const objetosVeteranosPath = "/api/objetos-veteranos";

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

export function listarObjetosRelacionadosAVeterano(veteranoId: number) {
  return apiRequest<ObjetoVeteranoResponseDTO[]>(`${objetosVeteranosPath}?veteranoId=${veteranoId}`);
}

export function listarRelacionesObjetoVeterano() {
  return apiRequest<ObjetoVeteranoResponseDTO[]>(objetosVeteranosPath);
}

export function asociarObjetoAVeterano(payload: ObjetoVeteranoRequestDTO) {
  return apiRequest<ObjetoVeteranoResponseDTO>(objetosVeteranosPath, { method: "POST", body: JSON.stringify(payload) });
}

export function eliminarRelacionObjetoVeterano(id: number) {
  return apiRequest<void>(`${objetosVeteranosPath}/${id}`, { method: "DELETE" });
}
