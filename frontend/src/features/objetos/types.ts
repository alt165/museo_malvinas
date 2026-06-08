import type { CategoriaObjetoResponseDTO } from "@/features/categorias/types";

export type EstadoConservacion = "EXCELENTE" | "BUENO" | "REGULAR" | "MALO" | "CRITICO";
export type OrigenCargaObjeto = "RAPIDA" | "COMPLETA";
export type CaracterRecepcionObjeto = "PRESTAMO" | "COMODATO" | "DONACION" | "COMPRA" | "ESTUDIO" | "OTRO" | "RECEPCION";

export type ObjetoMuseoRequestDTO = {
  numeroInventario: string;
  denominacionObjeto: string;
  descripcion?: string | null;
  descripcionTecnica?: string | null;
  materiales?: string | null;
  dimensiones?: string | null;
  estadoConservacion?: EstadoConservacion | null;
  categoriaIds?: number[];
  ubicacionId?: number | null;
  depositanteId?: number | null;
  caracterRecepcion?: CaracterRecepcionObjeto | null;
  fechaVencimiento?: string | null;
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
  origenCarga?: OrigenCargaObjeto | null;
  datosCompletos?: boolean | null;
  fechaCargaRapida?: string | null;
  cargaRapidaPor?: string | null;
  ubicacionId?: number | null;
  ubicacionNombre?: string | null;
  coleccionId?: number | null;
  coleccionNombre?: string | null;
  depositanteId?: number | null;
  depositanteNombre?: string | null;
  caracterRecepcion?: CaracterRecepcionObjeto | null;
  fechaVencimiento?: string | null;
  categorias?: CategoriaObjetoResponseDTO[];
  fotos?: FotoObjetoMuseoResponseDTO[];
  reciboEscaneado?: ReciboEscaneadoObjetoMuseoResponseDTO | null;
};

export type ObjetoVencimientoProximoResponseDTO = {
  id: number;
  numeroInventario: string;
  denominacionObjeto: string;
  depositanteId: number;
  depositanteNombre: string;
  caracterRecepcion: CaracterRecepcionObjeto;
  fechaVencimiento: string;
  diasRestantes: number;
};

export type EstadoVencimientoComodatoPrestamo = "VIGENTE" | "PROXIMO_A_VENCER" | "VENCIDO";

export type ComodatoPrestamoResponseDTO = {
  id: number;
  numeroInventario: string;
  denominacionObjeto: string;
  depositanteId: number;
  depositanteNombre: string;
  caracterRecepcion: Extract<CaracterRecepcionObjeto, "PRESTAMO" | "COMODATO">;
  fechaIngreso: string;
  fechaVencimiento?: string | null;
  diasRestantes?: number | null;
  estadoVencimiento: EstadoVencimientoComodatoPrestamo;
};

export type ConfigAlertasVencimientoDTO = {
  diasAnticipacion: number;
};

export type HistorialObjetoResponseDTO = {
  id: number;
  fechaHora: string;
  tipoOperacion: string;
  accion?: string | null;
  descripcion?: string | null;
  usuario?: string | null;
  rol?: string | null;
  origen?: string | null;
  valoresAnteriores?: string | null;
  valoresNuevos?: string | null;
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
export type ObjetoPendienteSortField = "fechaCargaRapida" | "numeroInventario" | "denominacionObjeto";

export type SortDirection = "asc" | "desc";

export type ObjetosSort = {
  field: ObjetoSortField;
  direction: SortDirection;
};

export type ObjetosPendientesSort = {
  field: ObjetoPendienteSortField;
  direction: SortDirection;
};

export type ObjetoPendienteCompletarResponseDTO = {
  id: number;
  numeroInventario: string;
  denominacionObjeto: string;
  descripcion?: string | null;
  depositanteId?: number | null;
  depositanteNombre?: string | null;
  caracterRecepcion?: CaracterRecepcionObjeto | null;
  fechaVencimiento?: string | null;
  fechaCargaRapida?: string | null;
  cargaRapidaPor?: string | null;
  reciboId?: number | null;
  reciboPdfUrl?: string | null;
};

export type FotoObjetoMuseoResponseDTO = {
  id: number;
  objetoMuseoId: number;
  nombreArchivo: string;
  nombreArchivoAlmacenado?: string | null;
  contentType: string;
  tamanioBytes: number;
  descripcion?: string | null;
  fechaCarga: string;
  cargadoPor?: string | null;
};

export type ReciboEscaneadoObjetoMuseoResponseDTO = {
  id: number;
  objetoMuseoId: number;
  nombreArchivoOriginal: string;
  contentType: string;
  tamanioBytes: number;
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

export type MoverObjetoRequestDTO = {
  ubicacionDestinoId: number;
  descripcion?: string | null;
};

export type MovimientoObjetoResponseDTO = {
  id: number;
  fechaMovimiento: string;
  ubicacionOrigenId?: number | null;
  ubicacionOrigen?: string | null;
  ubicacionDestinoId?: number | null;
  ubicacionDestino?: string | null;
  descripcion?: string | null;
  usuarioMovimiento?: string | null;
  tipoMovimiento?: string | null;
  estadoAnterior?: string | null;
  estadoNuevo?: string | null;
};
