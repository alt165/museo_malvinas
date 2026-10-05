export type ApiErrorResponse = {
  timestamp?: string;
  status: number;
  error?: string;
  message: string;
  path?: string;
  requestId?: string;
  validationErrors?: Record<string, string>;
  code?: string;
  depositanteId?: number;
  tipoIdentificacion?: "DNI" | "CUIT";
};
