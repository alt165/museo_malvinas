import type { ApiErrorResponse } from "@/models/api-error";

const DEFAULT_ERROR_MESSAGE = "No se pudo completar la operacion.";

const statusMessages: Record<number, string> = {
  400: "Revise los datos ingresados.",
  401: "Debe iniciar sesion nuevamente.",
  403: "No tiene permisos para realizar esta accion.",
  404: "No se encontro la informacion solicitada.",
  409: "No se pudo completar la operacion.",
  500: "Ocurrio un error inesperado. Intente nuevamente."
};

const technicalMessagePatterns = [
  /request\s*failed/i,
  /request\s*id/i,
  /trace\s*id/i,
  /^unauthorized$/i,
  /^forbidden$/i,
  /^internal\s+server\s+error$/i,
  /^bad\s+request$/i,
  /^not\s+found$/i,
  /^api\s+error$/i,
  /failed\s+to\s+fetch/i,
  /network\s*error/i
];

export class ApiClientError extends Error {
  readonly status: number;
  readonly requestId?: string;
  readonly payload?: ApiErrorResponse;

  constructor(payload: ApiErrorResponse) {
    super(getUserFacingApiErrorMessage(payload));
    this.name = "ApiClientError";
    this.status = payload.status;
    this.requestId = payload.requestId;
    this.payload = payload;
  }
}

export function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  if (!value || typeof value !== "object") {
    return false;
  }

  return "status" in value && "message" in value;
}

export function getUserFacingErrorMessage(error: unknown) {
  if (error instanceof ApiClientError) {
    return getUserFacingApiErrorMessage(error.payload);
  }

  if (error instanceof Error) {
    return sanitizeGenericMessage(error.message);
  }

  return DEFAULT_ERROR_MESSAGE;
}

function getUserFacingApiErrorMessage(payload?: ApiErrorResponse) {
  if (!payload) {
    return DEFAULT_ERROR_MESSAGE;
  }

  if (payload.status === 401 || payload.status === 403 || payload.status >= 500) {
    return getStatusMessage(payload.status);
  }

  if (payload.status === 400 && payload.validationErrors && Object.keys(payload.validationErrors).length > 0) {
    return statusMessages[400];
  }

  const message = sanitizeMessage(payload.message);

  if (message && !isTechnicalMessage(message)) {
    return message;
  }

  return getStatusMessage(payload.status);
}

function sanitizeGenericMessage(message?: string | null) {
  const sanitized = sanitizeMessage(message);

  if (!sanitized || isTechnicalMessage(sanitized)) {
    return DEFAULT_ERROR_MESSAGE;
  }

  return sanitized;
}

function sanitizeMessage(message?: string | null) {
  return message
    ?.replace(/\b(requestId|request id|traceId|trace id)\b\s*[:=]\s*[\w.-]+/gi, "")
    .replace(/\s{2,}/g, " ")
    .trim();
}

function isTechnicalMessage(message: string) {
  return technicalMessagePatterns.some((pattern) => pattern.test(message));
}

function getStatusMessage(status: number) {
  if (status >= 500) {
    return statusMessages[500];
  }

  return statusMessages[status] ?? DEFAULT_ERROR_MESSAGE;
}
