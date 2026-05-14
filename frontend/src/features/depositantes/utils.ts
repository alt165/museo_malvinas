import { ApiClientError } from "@/lib/errors/api-error";
import type { DepositanteRequestDTO, DepositanteResponseDTO } from "./types";
import type { DepositanteFormValues } from "./schemas";

const telefonoPrefix = "Telefono: ";
const direccionPrefix = "Direccion: ";
const observacionesPrefix = "Observaciones:";

export function getApiErrorMessage(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.message;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "Ocurrio un error inesperado";
}

export function getValidationErrors(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.payload?.validationErrors ?? {};
  }

  return {};
}

export function nombreVisible(depositante: DepositanteResponseDTO) {
  return depositante.nombre || "Sin nombre";
}

export function telefonoVisible(depositante: DepositanteResponseDTO) {
  return parseObservaciones(depositante.observaciones).telefono || "Sin telefono";
}

export function identificacionVisible(depositante: DepositanteResponseDTO) {
  return depositante.tipo === "PERSONA"
    ? depositante.dni || "Sin DNI"
    : depositante.cuit || "Sin CUIT";
}

export function resumenObservaciones(value?: string | null) {
  if (!value) {
    return "Sin observaciones";
  }

  return value.length > 160 ? `${value.slice(0, 157)}...` : value;
}

export function depositanteToFormValues(depositante?: DepositanteResponseDTO, initialIdentification?: string): DepositanteFormValues {
  const nombrePartes = depositante?.tipo === "PERSONA" ? splitNombrePersona(depositante.nombre) : { nombre: "", apellido: "" };
  const datosObservaciones = parseObservaciones(depositante?.observaciones);
  const identificacionInicial = initialIdentification?.trim() ?? "";
  const tipoInicial = depositante?.tipo ?? (soloDigitos(identificacionInicial).length === 11 ? "INSTITUCION" : "PERSONA");

  return {
    tipo: tipoInicial,
    nombre: nombrePartes.nombre,
    apellido: nombrePartes.apellido,
    organizacion: depositante?.tipo === "INSTITUCION" ? depositante.nombre : "",
    email: depositante?.contacto ?? "",
    dni: depositante?.dni ?? (tipoInicial === "PERSONA" ? identificacionInicial : ""),
    cuit: depositante?.cuit ?? (tipoInicial === "INSTITUCION" ? identificacionInicial : ""),
    telefono: datosObservaciones.telefono,
    direccion: datosObservaciones.direccion,
    observaciones: datosObservaciones.observaciones
  };
}

function soloDigitos(value: string) {
  return value.replace(/\D/g, "");
}

export function formValuesToDepositanteRequest(values: DepositanteFormValues): DepositanteRequestDTO {
  const nombre =
    values.tipo === "PERSONA"
      ? [values.nombre?.trim(), values.apellido?.trim()].filter(Boolean).join(" ")
      : values.organizacion?.trim() ?? "";

  return {
    tipo: values.tipo,
    nombre,
    contacto: values.email?.trim() || null,
    dni: values.tipo === "PERSONA" ? values.dni?.trim() || null : null,
    cuit: values.tipo === "INSTITUCION" ? values.cuit?.trim() || null : null,
    observaciones: buildObservaciones(values)
  };
}

function splitNombrePersona(value?: string | null) {
  const partes = (value ?? "").trim().split(/\s+/).filter(Boolean);

  if (partes.length <= 1) {
    return { nombre: partes[0] ?? "", apellido: "" };
  }

  return {
    nombre: partes[0],
    apellido: partes.slice(1).join(" ")
  };
}

function buildObservaciones(values: DepositanteFormValues) {
  const lines = [
    values.telefono?.trim() ? `${telefonoPrefix}${values.telefono.trim()}` : "",
    values.direccion?.trim() ? `${direccionPrefix}${values.direccion.trim()}` : "",
    values.observaciones?.trim() ? `${observacionesPrefix}\n${values.observaciones.trim()}` : ""
  ].filter(Boolean);

  return lines.length > 0 ? lines.join("\n") : null;
}

function parseObservaciones(value?: string | null) {
  if (!value) {
    return { telefono: "", direccion: "", observaciones: "" };
  }

  const lines = value.split("\n");
  const telefono = lines.find((line) => line.startsWith(telefonoPrefix))?.slice(telefonoPrefix.length) ?? "";
  const direccion = lines.find((line) => line.startsWith(direccionPrefix))?.slice(direccionPrefix.length) ?? "";
  const observacionesIndex = lines.findIndex((line) => line === observacionesPrefix);

  if (observacionesIndex >= 0) {
    return {
      telefono,
      direccion,
      observaciones: lines.slice(observacionesIndex + 1).join("\n")
    };
  }

  const sinCampos = lines
    .filter((line) => !line.startsWith(telefonoPrefix) && !line.startsWith(direccionPrefix))
    .join("\n")
    .trim();

  return { telefono, direccion, observaciones: sinCampos };
}
