import type { Fuerza } from "@/features/veteranos/types";

export type RangoMilitarRequestDTO = {
  fuerza: Fuerza;
  nombre: string;
  ordenJerarquico: number;
};

export type RangoMilitarResponseDTO = RangoMilitarRequestDTO & {
  id: number;
};

export type UnidadMilitarRequestDTO = {
  fuerza: Fuerza;
  nombre: string;
  sigla?: string | null;
  tipoUnidad?: string | null;
  descripcion?: string | null;
};

export type UnidadMilitarResponseDTO = UnidadMilitarRequestDTO & {
  id: number;
};

export type DetalleConservacionRequestDTO = {
  nombre: string;
  codigo?: string | null;
  descripcion?: string | null;
};

export type DetalleConservacionResponseDTO = {
  id: number;
  codigo: string;
  nombre: string;
  descripcion?: string | null;
};
