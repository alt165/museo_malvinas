export type EstadoInventario = "DISPONIBLE" | "EN_EXHIBICION" | "EN_RESTAURACION" | "PRESTADO" | "BAJA";

export type EstadoConservacion = "EXCELENTE" | "BUENO" | "REGULAR" | "MALO" | "CRITICO";

export type TipoMovimientoInventario =
  | "INGRESO"
  | "CAMBIO_UBICACION"
  | "SALIDA_EXHIBICION"
  | "DEVOLUCION_EXHIBICION"
  | "RESTAURACION"
  | "PRESTAMO"
  | "BAJA";

export type InventarioRequestDTO = {
  objetoMuseoId: number;
  ubicacionId: number;
  estado: EstadoInventario;
  estadoConservacion: EstadoConservacion;
  fechaIngreso: string;
  fechaSalida?: string | null;
  observaciones?: string | null;
};

export type InventarioResponseDTO = {
  id: number;
  objetoMuseoId: number;
  objetoNombre: string;
  ubicacionId: number;
  ubicacionNombre: string;
  estado: EstadoInventario;
  estadoConservacion: EstadoConservacion;
  fechaIngreso: string;
  fechaSalida?: string | null;
  fechaUltimoMovimiento?: string | null;
  observaciones?: string | null;
};

export type MovimientoInventarioResponseDTO = {
  id: number;
  objetoMuseoId: number;
  objetoNombre: string;
  tipo: TipoMovimientoInventario;
  fecha: string;
  ubicacionOrigenId?: number | null;
  ubicacionOrigenNombre?: string | null;
  ubicacionDestinoId?: number | null;
  ubicacionDestinoNombre?: string | null;
  usuarioId?: number | null;
  usuarioNombre?: string | null;
  observaciones?: string | null;
};

export const estadosInventario: EstadoInventario[] = [
  "DISPONIBLE",
  "EN_EXHIBICION",
  "EN_RESTAURACION",
  "PRESTADO",
  "BAJA"
];

export const estadosConservacion: EstadoConservacion[] = ["EXCELENTE", "BUENO", "REGULAR", "MALO", "CRITICO"];
