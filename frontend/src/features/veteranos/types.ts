export type Fuerza = "EJERCITO" | "ARMADA" | "FUERZA_AEREA" | "PREFECTURA" | "GENDARMERIA" | "CIVIL";

export type VeteranoRequestDTO = {
  nombre: string;
  apellido: string;
  fuerza: Fuerza;
  fechaNacimiento?: string | null;
  fechaFallecimiento?: string | null;
  historia?: string | null;
};

export type VeteranoResponseDTO = VeteranoRequestDTO & {
  id: number;
  nombreCompleto: string;
};

export type ActuacionVeteranoRequestDTO = {
  veteranoId: number;
  rango?: string | null;
  unidad?: string | null;
  rangoId?: number | null;
  unidadId?: number | null;
  rol?: string | null;
  fechaInicio?: string | null;
  fechaFin?: string | null;
  descripcion?: string | null;
};

export type ActuacionVeteranoResponseDTO = ActuacionVeteranoRequestDTO & {
  id: number;
  veteranoNombreCompleto: string;
  rangoNombre?: string | null;
  unidadNombre?: string | null;
  unidadSigla?: string | null;
};

export type RangoMilitarResponseDTO = {
  id: number;
  fuerza: Fuerza;
  nombre: string;
  ordenJerarquico: number;
};

export type UnidadMilitarResponseDTO = {
  id: number;
  fuerza: Fuerza;
  nombre: string;
  sigla?: string | null;
  tipoUnidad?: string | null;
  descripcion?: string | null;
};

export type ObjetoVeteranoRequestDTO = {
  objetoMuseoId: number;
  veteranoId: number;
  tipoRelacion: string;
  descripcion?: string | null;
};

export type ObjetoVeteranoResponseDTO = ObjetoVeteranoRequestDTO & {
  id: number;
  objetoNombre: string;
  veteranoNombreCompleto: string;
};

export const fuerzas: Fuerza[] = ["EJERCITO", "ARMADA", "FUERZA_AEREA", "PREFECTURA", "GENDARMERIA", "CIVIL"];
