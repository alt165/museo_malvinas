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
  rol?: string | null;
  fechaInicio?: string | null;
  fechaFin?: string | null;
  descripcion?: string | null;
};

export type ActuacionVeteranoResponseDTO = ActuacionVeteranoRequestDTO & {
  id: number;
  veteranoNombreCompleto: string;
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
