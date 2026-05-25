export type UbicacionResponseDTO = {
  id: number;
  nombre: string;
  descripcion?: string | null;
};

export type UbicacionRequestDTO = {
  nombre: string;
  descripcion?: string | null;
};
