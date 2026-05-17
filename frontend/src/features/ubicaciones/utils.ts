export function getApiErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  return "No se pudo completar la operacion.";
}
