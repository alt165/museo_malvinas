import { apiBlobRequest, apiRequest } from "@/lib/api";
import type {
  ActuacionVeteranoRequestDTO,
  ActuacionVeteranoResponseDTO,
  Fuerza,
  ObjetoVeteranoRequestDTO,
  ObjetoVeteranoResponseDTO,
  RangoMilitarResponseDTO,
  UnidadMilitarResponseDTO,
  VeteranoImagenResponseDTO,
  VeteranoRequestDTO,
  VeteranoResponseDTO,
  VeteranoVideoRequestDTO,
  VeteranoVideoResponseDTO
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

export function listarImagenesVeterano(id: number) {
  return apiRequest<VeteranoImagenResponseDTO[]>(`${veteranosPath}/${id}/imagenes`);
}

export function subirImagenesVeterano(id: number, archivos: File[], descripcion?: string) {
  const formData = new FormData();
  archivos.forEach((archivo) => formData.append("archivos", archivo));
  if (descripcion?.trim()) {
    formData.append("descripcion", descripcion.trim());
  }
  return apiRequest<VeteranoImagenResponseDTO[]>(`${veteranosPath}/${id}/imagenes`, {
    method: "POST",
    body: formData
  });
}

export function descargarImagenVeterano(id: number, imagenId: number) {
  return apiBlobRequest(`${veteranosPath}/${id}/imagenes/${imagenId}`);
}

export function eliminarImagenVeterano(id: number, imagenId: number) {
  return apiRequest<void>(`${veteranosPath}/${id}/imagenes/${imagenId}`, { method: "DELETE" });
}

export function listarVideosVeterano(id: number) {
  return apiRequest<VeteranoVideoResponseDTO[]>(`${veteranosPath}/${id}/videos`);
}

export function crearVideoVeterano(id: number, payload: VeteranoVideoRequestDTO) {
  return apiRequest<VeteranoVideoResponseDTO>(`${veteranosPath}/${id}/videos`, { method: "POST", body: JSON.stringify(payload) });
}

export function actualizarVideoVeterano(id: number, videoId: number, payload: VeteranoVideoRequestDTO) {
  return apiRequest<VeteranoVideoResponseDTO>(`${veteranosPath}/${id}/videos/${videoId}`, { method: "PUT", body: JSON.stringify(payload) });
}

export function eliminarVideoVeterano(id: number, videoId: number) {
  return apiRequest<void>(`${veteranosPath}/${id}/videos/${videoId}`, { method: "DELETE" });
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
