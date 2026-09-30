import type { DepositanteResponseDTO } from "@/features/depositantes/types";

const storageKey = "museo:carga-rapida:alta-depositante:v1";
const contextLifetimeMs = 2 * 60 * 60 * 1000;

export type CargaRapidaDepositanteContext = {
  version: 1;
  flowId: string;
  savedAt: number;
  form: {
    denominacionObjeto: string;
    descripcionBreve: string;
  };
  identificacion: string;
  nombreDepositante: string;
  depositanteCreado?: DepositanteResponseDTO;
};

export function createCargaRapidaDepositanteContext(input: Omit<CargaRapidaDepositanteContext, "version" | "flowId" | "savedAt">) {
  const context: CargaRapidaDepositanteContext = {
    ...input,
    version: 1,
    flowId: crypto.randomUUID(),
    savedAt: Date.now()
  };

  sessionStorage.setItem(storageKey, JSON.stringify(context));
  return context;
}

export function readCargaRapidaDepositanteContext(flowId: string | null) {
  if (!flowId) {
    return null;
  }

  const rawContext = sessionStorage.getItem(storageKey);
  if (!rawContext) {
    return null;
  }

  try {
    const context = JSON.parse(rawContext) as Partial<CargaRapidaDepositanteContext>;
    const isExpired = typeof context.savedAt !== "number" || Date.now() - context.savedAt > contextLifetimeMs;
    const isValid =
      context.version === 1 &&
      context.flowId === flowId &&
      typeof context.form?.denominacionObjeto === "string" &&
      typeof context.form?.descripcionBreve === "string" &&
      typeof context.identificacion === "string" &&
      typeof context.nombreDepositante === "string" &&
      (context.depositanteCreado === undefined || isDepositante(context.depositanteCreado));

    if (isExpired) {
      sessionStorage.removeItem(storageKey);
      return null;
    }

    return isValid ? (context as CargaRapidaDepositanteContext) : null;
  } catch {
    sessionStorage.removeItem(storageKey);
    return null;
  }
}

export function saveCreatedDepositante(context: CargaRapidaDepositanteContext, depositante: DepositanteResponseDTO) {
  sessionStorage.setItem(storageKey, JSON.stringify({ ...context, depositanteCreado: depositante }));
}

export function clearCargaRapidaDepositanteContext() {
  sessionStorage.removeItem(storageKey);
}

function isDepositante(value: unknown): value is DepositanteResponseDTO {
  if (!value || typeof value !== "object") {
    return false;
  }

  const depositante = value as Partial<DepositanteResponseDTO>;
  return (
    typeof depositante.id === "number" &&
    depositante.id > 0 &&
    typeof depositante.nombre === "string" &&
    (depositante.tipo === "PERSONA" || depositante.tipo === "INSTITUCION")
  );
}
