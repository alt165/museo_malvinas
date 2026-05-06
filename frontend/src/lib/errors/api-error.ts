import type { ApiErrorResponse } from "@/models/api-error";

export class ApiClientError extends Error {
  readonly status: number;
  readonly requestId?: string;
  readonly payload?: ApiErrorResponse;

  constructor(payload: ApiErrorResponse) {
    super(payload.message);
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
