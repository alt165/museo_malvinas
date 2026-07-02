import type { CategoriaObjetoResponseDTO } from "@/features/categorias/types";

export type EstadoConservacion = "EXCELENTE" | "BUENO" | "REGULAR" | "MALO" | "CRITICO";
export type OrigenCargaObjeto = "RAPIDA" | "COMPLETA";
export type CaracterRecepcionObjeto = "PRESTAMO" | "COMODATO" | "DONACION" | "COMPRA" | "ESTUDIO" | "OTRO" | "RECEPCION";
export type DetalleEstadoConservacion = "GRIETAS" | "RASGADURAS" | "HONGOS" | "HUNDIMIENTOS" | "HUELLAS_DE_HUMEDAD" | "DESGASTE" | "DESPRENDIMIENTOS" | "ADHESION_DE_HOJAS" | "SOBREPINTURA" | "FRACTURAS" | "DOBLECES" | "MARCAS" | "QUEMADURAS" | "DESFASES" | "OXIDACION_DE_TINTA" | "PERDIDA_DE_TINTA" | "DESENCUADERNADO" | "ROTURA" | "POLVO" | "DESTENSADOS" | "INSECTOS" | "CRAQUELADOS" | "DEFORMACIONES" | "FALTA_DE_ADHESION" | "FALTANTE_DE_SOPORTE" | "FALTANTE_DE_TAPA" | "FALTANTE_DE_CUERPO" | "FALTANTE_DE_LOMO" | "DECOLORACION" | "DESCOSIDO" | "ABOLSADOS" | "LAGUNAS" | "OXIDACION" | "MICROORGANISMOS" | "SUCIEDAD_SUPERFICIAL" | "FALTANTE" | "MANCHAS" | "ANIMALES_MENORES" | "EXFOLIACIONES" | "SALES" | "GOLPES" | "RAYADURAS" | "SUCIO";
export type RegimenPropiedad = "PUBLICO" | "PRIVADO";
export type IntervencionesInadecuadas = "SI" | "NO" | "ELEMENTOS_EXTRANOS";
export type EstadoIntegridad = "COMPLETO" | "INCOMPLETO" | "FRAGMENTADO";
export type HumedadConservacion = "ALTA" | "BAJA";
export type VisibilidadCampo = "PUBLICO" | "PRIVADO";
export type VisibilidadesObjeto = Record<string, VisibilidadCampo>;

export type ObjetoMuseoRequestDTO = {
  numeroInventario: string;
  denominacionObjeto: string;
  descripcion?: string | null;
  descripcionTecnica?: string | null;
  materiales?: string | null;
  alto?: string | null;
  ancho?: string | null;
  diametro?: string | null;
  espesor?: string | null;
  peso?: string | null;
  inscripciones?: string | null;
  regimenPropiedad?: RegimenPropiedad | null;
  condicionLegalBien?: string | null;
  estadoConservacion?: EstadoConservacion | null;
  detallesEstadoConservacion?: DetalleEstadoConservacion[];
  intervencionesInadecuadas?: IntervencionesInadecuadas | null;
  estadoIntegridad?: EstadoIntegridad | null;
  humedadConservacion?: HumedadConservacion | null;
  temperaturaConservacion?: string | null;
  luzConservacion?: string | null;
  conservacionExtintores?: boolean | null;
  conservacionMontaje?: boolean | null;
  conservacionSistemaElectrico?: boolean | null;
  conservacionAlarmas?: boolean | null;
  conservacionCamaras?: boolean | null;
  visibilidades?: VisibilidadesObjeto;
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
  alto?: string | null;
  ancho?: string | null;
  diametro?: string | null;
  espesor?: string | null;
  peso?: string | null;
  inscripciones?: string | null;
  regimenPropiedad?: RegimenPropiedad | null;
  condicionLegalBien?: string | null;
  estadoConservacion?: EstadoConservacion | null;
  detallesEstadoConservacion?: DetalleEstadoConservacion[];
  intervencionesInadecuadas?: IntervencionesInadecuadas | null;
  estadoIntegridad?: EstadoIntegridad | null;
  humedadConservacion?: HumedadConservacion | null;
  temperaturaConservacion?: string | null;
  luzConservacion?: string | null;
  conservacionExtintores?: boolean | null;
  conservacionMontaje?: boolean | null;
  conservacionSistemaElectrico?: boolean | null;
  conservacionAlarmas?: boolean | null;
  conservacionCamaras?: boolean | null;
  visibilidades?: VisibilidadesObjeto;
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

export type BuscarObjetosDisponiblesColeccionParams = BuscarObjetosParams & {
  coleccionId?: number;
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
  visibilidad: VisibilidadCampo;
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

export type EmbargoObjetoRequestDTO = {
  objetoMuseoId: number;
  fechaInicio?: string | null;
  fechaFinalizacion?: string | null;
  observaciones?: string | null;
};

export type EstadoEmbargoObjeto = "VIGENTE" | "LEVANTADO";

export type EmbargoObjetoResponseDTO = {
  id: number;
  objetoMuseoId: number;
  numeroInventario: string;
  denominacionObjeto: string;
  fechaInicio: string;
  fechaFinalizacion?: string | null;
  estado: EstadoEmbargoObjeto;
  observaciones?: string | null;
};
