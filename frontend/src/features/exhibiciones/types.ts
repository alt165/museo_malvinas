export type TipoExhibicion = "TEMPORAL" | "PERMANENTE";

export type EstadoExhibicion = "PLANIFICADA" | "ACTIVA" | "FINALIZADA";

export type EstadoExhibicionObjeto = "EN_EXHIBICION" | "DEVUELTO" | "PENDIENTE_REVISION";

export type ExhibicionRequestDTO = {
  nombre: string;
  descripcion?: string | null;
  tipo: TipoExhibicion;
  fechaInicio: string;
  fechaFin?: string | null;
  estado: EstadoExhibicion;
};

export type ExhibicionResponseDTO = ExhibicionRequestDTO & {
  id: number;
};

export type ExhibicionObjetoRequestDTO = {
  exhibicionId: number;
  objetoMuseoId: number;
  fechaInclusion: string;
  fechaRetiro?: string | null;
  estado: EstadoExhibicionObjeto;
  devolucionVerificada?: boolean | null;
  verificadoPorUsuarioId?: number | null;
  fechaVerificacion?: string | null;
  observacionesDevolucion?: string | null;
};

export type ExhibicionObjetoResponseDTO = {
  id: number;
  exhibicionId: number;
  exhibicionNombre: string;
  objetoMuseoId: number;
  objetoNombre: string;
  fechaInclusion: string;
  fechaRetiro?: string | null;
  estado: EstadoExhibicionObjeto;
  devolucionVerificada: boolean;
  verificadoPorUsuarioId?: number | null;
  verificadoPorNombre?: string | null;
  fechaVerificacion?: string | null;
  observacionesDevolucion?: string | null;
};

export const tiposExhibicion: TipoExhibicion[] = ["TEMPORAL", "PERMANENTE"];
export const estadosExhibicion: EstadoExhibicion[] = ["PLANIFICADA", "ACTIVA", "FINALIZADA"];
