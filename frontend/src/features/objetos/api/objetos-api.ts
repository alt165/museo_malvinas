import { apiBlobRequest, apiRequest } from "@/lib/api";
import type {
  CargaRapidaObjetoRequestDTO,
  CargaRapidaObjetoResponseDTO,
  FotoObjetoMuseoResponseDTO,
  BuscarObjetosParams,
  ObjetoMuseoRequestDTO,
  ObjetoMuseoResponseDTO,
  PageResponse,
  ReciboIngresoObjetoResponseDTO
} from "../types";

const basePath = "/api/objetos";

export function listarObjetos() {
  return apiRequest<ObjetoMuseoResponseDTO[]>(basePath);
}

export function buscarObjetos(params: BuscarObjetosParams) {
  const searchParams = new URLSearchParams();

  if (params.nombre?.trim()) {
    searchParams.set("nombre", params.nombre.trim());
  }

  if (params.numeroInventario?.trim()) {
    searchParams.set("numeroInventario", params.numeroInventario.trim());
  }

  params.categoriaIds?.forEach((categoriaId) => {
    searchParams.append("categoriaIds", String(categoriaId));
  });

  searchParams.set("page", String(params.page ?? 0));
  searchParams.set("size", String(params.size ?? 20));

  if (params.sort) {
    searchParams.set("sort", params.sort);
  }

  return apiRequest<PageResponse<ObjetoMuseoResponseDTO>>(`${basePath}/buscar?${searchParams.toString()}`);
}

export function obtenerObjetoPorId(id: number) {
  return apiRequest<ObjetoMuseoResponseDTO>(`${basePath}/${id}`);
}

export function crearObjeto(payload: ObjetoMuseoRequestDTO) {
  return apiRequest<ObjetoMuseoResponseDTO>(basePath, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function actualizarObjeto(id: number, payload: ObjetoMuseoRequestDTO) {
  return apiRequest<ObjetoMuseoResponseDTO>(`${basePath}/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function bajaLogicaObjeto(id: number) {
  return apiRequest<void>(`${basePath}/${id}`, {
    method: "DELETE"
  });
}

export function cargaRapidaObjeto(payload: CargaRapidaObjetoRequestDTO) {
  return apiRequest<CargaRapidaObjetoResponseDTO>(`${basePath}/carga-rapida`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function listarFotosObjeto(id: number) {
  return apiRequest<FotoObjetoMuseoResponseDTO[]>(`${basePath}/${id}/fotos`);
}

export function subirFotoObjeto(id: number, archivo: File, descripcion?: string) {
  const formData = new FormData();
  formData.append("archivo", archivo);
  if (descripcion) {
    formData.append("descripcion", descripcion);
  }
  return apiRequest<FotoObjetoMuseoResponseDTO>(`${basePath}/${id}/fotos`, {
    method: "POST",
    body: formData
  });
}

export function eliminarFotoObjeto(id: number, fotoId: number) {
  return apiRequest<void>(`${basePath}/${id}/fotos/${fotoId}`, {
    method: "DELETE"
  });
}

export function descargarFotoObjeto(id: number, fotoId: number) {
  return apiBlobRequest(`${basePath}/${id}/fotos/${fotoId}`);
}

export function listarRecibosObjeto(id: number) {
  return apiRequest<ReciboIngresoObjetoResponseDTO[]>(`${basePath}/${id}/recibos`);
}

export function descargarReciboPdf(id: number) {
  return apiBlobRequest(`/api/recibos/${id}/pdf`);
}

export function subirCopiaFirmadaRecibo(id: number, archivo: File) {
  const formData = new FormData();
  formData.append("archivo", archivo);
  return apiRequest<ReciboIngresoObjetoResponseDTO>(`/api/recibos/${id}/copia-firmada`, {
    method: "POST",
    body: formData
  });
}

export function descargarCopiaFirmadaRecibo(id: number) {
  return apiBlobRequest(`/api/recibos/${id}/copia-firmada`);
}
