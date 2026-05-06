import { ApiClientError, isApiErrorResponse } from "@/lib/errors/api-error";
import { getAccessToken, redirectToLogin } from "@/lib/auth/session";
import type { ApiErrorResponse } from "@/models/api-error";

type RequestOptions = RequestInit & {
  auth?: boolean;
  token?: string | null;
};

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

async function parseErrorResponse(response: Response): Promise<ApiErrorResponse> {
  const requestId = response.headers.get("X-Request-Id") ?? undefined;

  try {
    const payload: unknown = await response.json();

    if (isApiErrorResponse(payload)) {
      return {
        ...payload,
        requestId: payload.requestId ?? requestId
      };
    }
  } catch {
    // Fall through to a generic error below when the backend sends no JSON body.
  }

  return {
    status: response.status,
    error: response.statusText,
    message: response.statusText || "Error de API",
    requestId
  };
}

export async function apiRequest<TResponse>(
  path: string,
  options: RequestOptions = {}
): Promise<TResponse> {
  const headers = new Headers(options.headers);
  const { auth = true, token, ...requestOptions } = options;
  headers.set("Accept", "application/json");

  if (requestOptions.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const accessToken = token ?? (auth ? await getAccessToken() : null);

  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...requestOptions,
    headers
  });

  if (response.status === 401 && auth) {
    await redirectToLogin();
  }

  if (!response.ok) {
    throw new ApiClientError(await parseErrorResponse(response));
  }

  if (response.status === 204) {
    return undefined as TResponse;
  }

  return response.json() as Promise<TResponse>;
}
