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

export type RelacionObjetoPorObjetoResponseDTO = {
  idRelacion: number;
  objetoOrigenId: number;
  objetoOrigenNumeroInventario: string;
  objetoOrigenNombre: string;
  objetoDestinoId: number;
  objetoDestinoNumeroInventario: string;
  objetoDestinoNombre: string;
  tipoRelacion: string;
  descripcion?: string | null;
  direccion: "SALIENTE" | "ENTRANTE";
};

export type NodoGrafoObjetoDTO = {
  id: number;
  label: string;
  numeroInventario: string;
};

export type AristaGrafoObjetoDTO = {
  id: number;
  source: number;
  target: number;
  tipoRelacion: string;
  descripcion?: string | null;
};

export type ObjetoGrafoResponseDTO = {
  nodes: NodoGrafoObjetoDTO[];
  edges: AristaGrafoObjetoDTO[];
};
