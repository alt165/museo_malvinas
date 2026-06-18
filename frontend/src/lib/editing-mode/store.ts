let editingModeEnabled = false;

export function isEditingModeEnabled() {
  return editingModeEnabled;
}

export function setEditingModeEnabled(enabled: boolean) {
  editingModeEnabled = enabled;
}

export function isMutationMethod(method?: string) {
  const normalized = (method ?? "GET").toUpperCase();
  return normalized !== "GET" && normalized !== "HEAD" && normalized !== "OPTIONS";
}
