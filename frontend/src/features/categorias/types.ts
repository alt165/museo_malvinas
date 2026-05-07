export type CategoriaObjetoResponseDTO = {
  id: number;
  nombre: string;
  descripcion?: string | null;
  activo?: boolean;
};

export type CategoriaObjetoRequestDTO = {
  nombre: string;
  descripcion?: string | null;
};
