import { ApiClientError, getUserFacingErrorMessage } from "@/lib/errors/api-error";

export function getApiErrorMessage(error: unknown) {
  return getUserFacingErrorMessage(error);
}

export function getValidationErrors(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.payload?.validationErrors ?? {};
  }

  return {};
}

export function resumenDescripcion(value?: string | null) {
  if (!value) {
    return "Sin descripcion";
  }

  return value.length > 120 ? `${value.slice(0, 117)}...` : value;
}
