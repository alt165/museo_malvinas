export type ObjetoMuseoRequestDTO = {
  numeroInventario: string;
  nombre: string;
  tipoObjeto?: string | null;
  descripcion?: string | null;
};

export type ObjetoMuseoResponseDTO = {
  id: number;
  numeroInventario: string;
  nombre: string;
  tipoObjeto?: string | null;
  descripcion?: string | null;
};
