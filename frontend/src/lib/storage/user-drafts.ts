export const USER_DRAFT_PREFIX = "museo:user-draft:";

export function objectCreateDraftKey(userId: string) {
  return `${USER_DRAFT_PREFIX}${encodeURIComponent(userId)}:objeto-alta-completa`;
}

export function clearUserDrafts(userId?: string) {
  if (typeof window === "undefined") return;
  const prefix = userId ? `${USER_DRAFT_PREFIX}${encodeURIComponent(userId)}:` : USER_DRAFT_PREFIX;
  for (let index = window.localStorage.length - 1; index >= 0; index -= 1) {
    const key = window.localStorage.key(index);
    if (key?.startsWith(prefix)) window.localStorage.removeItem(key);
  }
}
