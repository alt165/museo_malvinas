export type ColeccionObjetoRequestDTO = {
  nombre: string;
  descripcion?: string | null;
};

export type ColeccionObjetoResponseDTO = {
  id: number;
  nombre: string;
  descripcion?: string | null;
  activo?: boolean | null;
  cantidadObjetos?: number | null;
};

export type AgregarObjetosColeccionRequestDTO = {
  objetoIds: number[];
};
