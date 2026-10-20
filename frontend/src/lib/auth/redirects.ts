export function safeReturnPath(value: string | null | undefined, fallback = "/dashboard"): string {
  if (!value || !value.startsWith("/") || value.startsWith("//") || value.includes("\\")) return fallback;
  try {
    const parsed = new URL(value, "https://same-origin.invalid");
    return parsed.origin === "https://same-origin.invalid" ? `${parsed.pathname}${parsed.search}${parsed.hash}` : fallback;
  } catch { return fallback; }
}

export function loginRedirectUri(origin: string, returnPath?: string | null): string { return `${origin}${safeReturnPath(returnPath)}`; }
export function logoutRedirectUri(origin: string): string { return `${origin}/login`; }
export function protectedRouteTarget(pathname: string, search: string): string { return `/login?redirect=${encodeURIComponent(safeReturnPath(`${pathname}${search}`))}`; }
