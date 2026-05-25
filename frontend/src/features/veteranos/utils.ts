import { ApiClientError, getUserFacingErrorMessage } from "@/lib/errors/api-error";

export function getApiErrorMessage(error: unknown) {
  return getUserFacingErrorMessage(error);
}

export function getValidationErrors(error: unknown) {
  if (error instanceof ApiClientError) return error.payload?.validationErrors ?? {};
  return {};
}

export function formatDate(value?: string | null) {
  if (!value) return "No registrada";
  return new Intl.DateTimeFormat("es-AR").format(new Date(`${value}T00:00:00`));
}
