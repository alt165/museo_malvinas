export type TipoDepositante = "PERSONA" | "INSTITUCION";

export type DepositanteRequestDTO = {
  nombre: string;
  tipo: TipoDepositante;
  contacto?: string | null;
  dni?: string | null;
  cuit?: string | null;
  observaciones?: string | null;
};

export type DepositanteResponseDTO = DepositanteRequestDTO & {
  id: number;
  activo?: boolean;
};

export const tiposDepositante: TipoDepositante[] = ["PERSONA", "INSTITUCION"];
