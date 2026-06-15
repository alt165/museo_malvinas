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
  objetoIds?: number[];
};

export type ExhibicionResponseDTO = ExhibicionRequestDTO & {
  id: number;
  permanente: boolean;
  objetos: ExhibicionObjetoResponseDTO[];
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
  objetoNumeroInventario?: string | null;
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


export type ObjetoDisponibilidadExhibicionResponseDTO = {
  objetoId: number;
  numeroInventario: string;
  denominacion: string;
  disponible: boolean;
  motivoNoDisponible?: string | null;
  exhibicionConflictoId?: number | null;
  exhibicionConflictoNombre?: string | null;
  exhibicionConflictoFechaInicio?: string | null;
  exhibicionConflictoFechaFin?: string | null;
  exhibicionConflictoPermanente: boolean;
};
