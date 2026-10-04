export type RelacionObjetoRequestDTO = {
  objetoOrigenId: number;
  objetoDestinoId: number;
  tipoRelacion: string;
  descripcion?: string | null;
};

export type RelacionObjetoResponseDTO = RelacionObjetoRequestDTO & {
  id: number;
  objetoOrigenNumeroInventario?: string | null;
  objetoOrigenNombre: string;
  objetoDestinoNumeroInventario?: string | null;
  objetoDestinoNombre: string;
  fechaCreacion?: string | null;
  creadoPor?: string | null;
  activo?: boolean;
};

export type TipoNodoRelacion = "OBJETO" | "PERSONA";
export type TipoVinculoRelacion = "OBJETO_OBJETO" | "OBJETO_PERSONA";

export type RelacionElementoResponseDTO = {
  idRelacion: string;
  tipoVinculo: TipoVinculoRelacion;
  tipoElemento: TipoNodoRelacion;
  elementoId: number;
  numeroInventario?: string | null;
  denominacion: string;
  tipoRelacion: string;
  descripcion?: string | null;
  direccion: "SALIENTE" | "ENTRANTE" | "VINCULADA" | "VINCULADO";
};

export type NodoGrafoObjetoDTO = {
  id: string;
  entidadId: number;
  tipo: TipoNodoRelacion;
  label: string;
  numeroInventario?: string | null;
};

export type AristaGrafoObjetoDTO = {
  id: string;
  source: string;
  target: string;
  tipoVinculo: TipoVinculoRelacion;
  tipoRelacion: string;
  descripcion?: string | null;
};

export type ObjetoGrafoResponseDTO = {
  nodes: NodoGrafoObjetoDTO[];
  edges: AristaGrafoObjetoDTO[];
};
