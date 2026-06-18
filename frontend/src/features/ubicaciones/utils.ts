import { getUserFacingErrorMessage } from "@/lib/errors/api-error";

export function getApiErrorMessage(error: unknown) {
  return getUserFacingErrorMessage(error);
}
