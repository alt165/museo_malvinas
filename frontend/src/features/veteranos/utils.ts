import type { Fuerza, VeteranoResponseDTO } from "./types";

export const fuerzaLabels: Record<Fuerza, string> = {
  EJERCITO: "Ejercito",
  ARMADA: "Armada",
  FUERZA_AEREA: "Fuerza Aerea",
  PREFECTURA: "Prefectura",
  GENDARMERIA: "Gendarmeria",
  CIVIL: "Civil"
};

export function fuerzaLabel(fuerza: Fuerza) {
  return fuerzaLabels[fuerza] ?? fuerza;
}

export function normalizarBusquedaVeterano(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLocaleLowerCase("es")
    .trim();
}

export function veteranoCoincideConBusqueda(veterano: VeteranoResponseDTO, valorBusqueda: string) {
  const valor = normalizarBusquedaVeterano(valorBusqueda);
  if (!valor) {
    return true;
  }

  return [veterano.nombre, veterano.apellido, veterano.fuerza, fuerzaLabel(veterano.fuerza)]
    .some((item) => normalizarBusquedaVeterano(item).includes(valor));
}

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
