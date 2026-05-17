export type UbicacionResponseDTO = {
  id: number;
  nombre: string;
  tipo?: string | null;
  descripcion?: string | null;
};

export type UbicacionRequestDTO = {
  nombre: string;
  tipo?: string | null;
  descripcion?: string | null;
};
