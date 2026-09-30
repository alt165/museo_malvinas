import { ApiClientError, getUserFacingErrorMessage } from "@/lib/errors/api-error";
import { formatDateTimeArgentina } from "@/lib/date-time";

export function getApiErrorMessage(error: unknown) {
  return getUserFacingErrorMessage(error);
}

export function getValidationErrors(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.payload?.validationErrors ?? {};
  }

  return {};
}

export function formatDate(value?: string | null) {
  if (!value) {
    return "No registrada";
  }

  return new Intl.DateTimeFormat("es-AR").format(new Date(`${value}T00:00:00`));
}

export function formatDateTime(value?: string | null) {
  return formatDateTimeArgentina(value, "No registrado");
}
