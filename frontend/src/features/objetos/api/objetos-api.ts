import { apiBlobRequest, apiRequest } from "@/lib/api";
import type {
  CargaRapidaObjetoRequestDTO,
  CargaRapidaObjetoResponseDTO,
  FotoObjetoMuseoResponseDTO,
  BuscarObjetosParams,
  MoverObjetoRequestDTO,
  MovimientoObjetoResponseDTO,
  ObjetoPendienteCompletarResponseDTO,
  ObjetoMuseoEliminadoResponseDTO,
  ObjetoMuseoRequestDTO,
  ObjetoMuseoResponseDTO,
  PageResponse,
  ReciboEscaneadoObjetoMuseoResponseDTO,
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

export function listarObjetosPendientesCompletar(params: { page?: number; size?: number; sort?: string }) {
  const searchParams = new URLSearchParams();
  searchParams.set("page", String(params.page ?? 0));
  searchParams.set("size", String(params.size ?? 20));
  if (params.sort) {
    searchParams.set("sort", params.sort);
  }
  return apiRequest<PageResponse<ObjetoPendienteCompletarResponseDTO>>(`${basePath}/pendientes-completar?${searchParams.toString()}`);
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

export function listarObjetosEliminados(params: { page?: number; size?: number; sort?: string }) {
  const searchParams = new URLSearchParams();
  searchParams.set("page", String(params.page ?? 0));
  searchParams.set("size", String(params.size ?? 20));
  if (params.sort) {
    searchParams.set("sort", params.sort);
  }
  return apiRequest<PageResponse<ObjetoMuseoEliminadoResponseDTO>>(`/api/admin/objetos/eliminados?${searchParams.toString()}`);
}

export function restaurarObjeto(id: number) {
  return apiRequest<ObjetoMuseoResponseDTO>(`/api/admin/objetos/${id}/restaurar`, {
    method: "POST"
  });
}

export function cargaRapidaObjeto(payload: CargaRapidaObjetoRequestDTO) {
  return apiRequest<CargaRapidaObjetoResponseDTO>(`${basePath}/carga-rapida`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function moverObjeto(id: number, payload: MoverObjetoRequestDTO) {
  return apiRequest<MovimientoObjetoResponseDTO>(`${basePath}/${id}/mover`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function listarMovimientosObjeto(id: number) {
  return apiRequest<MovimientoObjetoResponseDTO[]>(`${basePath}/${id}/movimientos`);
}

export function listarFotosObjeto(id: number) {
  return apiRequest<FotoObjetoMuseoResponseDTO[]>(`${basePath}/${id}/fotos`);
}

export function subirFotosObjeto(id: number, archivos: File[], descripcion?: string) {
  const formData = new FormData();
  archivos.forEach((archivo) => formData.append("archivos", archivo));
  if (descripcion) {
    formData.append("descripcion", descripcion);
  }
  return apiRequest<FotoObjetoMuseoResponseDTO[]>(`${basePath}/${id}/fotos`, {
    method: "POST",
    body: formData
  });
}

export async function subirFotoObjeto(id: number, archivo: File, descripcion?: string) {
  const [foto] = await subirFotosObjeto(id, [archivo], descripcion);
  return foto;
}

export function eliminarFotoObjeto(id: number, fotoId: number) {
  return apiRequest<void>(`${basePath}/${id}/fotos/${fotoId}`, {
    method: "DELETE"
  });
}

export function descargarFotoObjeto(id: number, fotoId: number) {
  return apiBlobRequest(`${basePath}/${id}/fotos/${fotoId}`);
}

export function obtenerReciboEscaneadoObjeto(id: number) {
  return apiRequest<ReciboEscaneadoObjetoMuseoResponseDTO>(`${basePath}/${id}/recibo-escaneado`);
}

export function subirReciboEscaneadoObjeto(id: number, archivo: File) {
  const formData = new FormData();
  formData.append("archivo", archivo);
  return apiRequest<ReciboEscaneadoObjetoMuseoResponseDTO>(`${basePath}/${id}/recibo-escaneado`, {
    method: "POST",
    body: formData
  });
}

export function descargarReciboEscaneadoObjeto(id: number) {
  return apiBlobRequest(`${basePath}/${id}/recibo-escaneado/archivo`);
}

export function eliminarReciboEscaneadoObjeto(id: number, archivoId: number) {
  return apiRequest<void>(`${basePath}/${id}/recibo-escaneado/${archivoId}`, {
    method: "DELETE"
  });
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
