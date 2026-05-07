export type RelacionObjetoRequestDTO = {
  objetoOrigenId: number;
  objetoDestinoId: number;
  tipoRelacion: string;
  descripcion?: string | null;
};

export type RelacionObjetoResponseDTO = RelacionObjetoRequestDTO & {
  id: number;
  objetoOrigenNombre: string;
  objetoDestinoNombre: string;
  activo?: boolean;
};
