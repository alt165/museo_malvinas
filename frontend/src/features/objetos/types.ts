import type { CategoriaObjetoResponseDTO } from "@/features/categorias/types";

export type EstadoConservacion = "EXCELENTE" | "BUENO" | "REGULAR" | "MALO" | "CRITICO";

export type ObjetoMuseoRequestDTO = {
  numeroInventario: string;
  denominacionObjeto: string;
  descripcion?: string | null;
  descripcionTecnica?: string | null;
  materiales?: string | null;
  dimensiones?: string | null;
  estadoConservacion?: EstadoConservacion | null;
  categoriaIds?: number[];
};

export type ObjetoMuseoResponseDTO = {
  id: number;
  numeroInventario: string;
  denominacionObjeto: string;
  descripcion?: string | null;
  descripcionTecnica?: string | null;
  materiales?: string | null;
  dimensiones?: string | null;
  estadoConservacion?: EstadoConservacion | null;
  fechaIngreso?: string | null;
  categorias?: CategoriaObjetoResponseDTO[];
};

export type ObjetoMuseoEliminadoResponseDTO = {
  id: number;
  numeroInventario: string;
  denominacionObjeto: string;
  descripcion?: string | null;
  fechaEliminacion?: string | null;
  eliminadoPor?: string | null;
  estadoConservacion?: EstadoConservacion | null;
  categorias?: CategoriaObjetoResponseDTO[];
};

export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
};

export type BuscarObjetosParams = {
  nombre?: string;
  numeroInventario?: string;
  categoriaIds?: number[];
  page?: number;
  size?: number;
  sort?: string;
};

export type ObjetoSortField = "numeroInventario" | "denominacionObjeto" | "descripcion" | "fechaIngreso" | "estadoConservacion";

export type SortDirection = "asc" | "desc";

export type ObjetosSort = {
  field: ObjetoSortField;
  direction: SortDirection;
};

export type FotoObjetoMuseoResponseDTO = {
  id: number;
  objetoMuseoId: number;
  nombreArchivo: string;
  contentType: string;
  tamanioBytes: number;
  descripcion?: string | null;
  fechaCarga: string;
  cargadoPor?: string | null;
};

export type CargaRapidaObjetoRequestDTO = {
  depositanteId: number;
  denominacionObjeto: string;
  numeroInventario: string;
  descripcionBreve: string;
};

export type ReciboIngresoObjetoResponseDTO = {
  id: number;
  numeroRecibo: string;
  fechaEmision: string;
  objetoMuseoId: number;
  depositanteId: number;
  numeroInventario: string;
  denominacionObjeto: string;
  descripcionBreve: string;
  depositanteNombre: string;
  depositanteContacto?: string | null;
  operador?: string | null;
  textoConstancia: string;
  tieneCopiaFirmada: boolean;
  copiaFirmadaNombreArchivo?: string | null;
  copiaFirmadaFechaCarga?: string | null;
  copiaFirmadaCargadoPor?: string | null;
};

export type CargaRapidaObjetoResponseDTO = {
  objeto: ObjetoMuseoResponseDTO;
  recibo: ReciboIngresoObjetoResponseDTO;
  reciboPdfUrl: string;
};
