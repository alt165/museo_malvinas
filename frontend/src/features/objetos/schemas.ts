import { z } from "zod";

const caracteresConVencimiento = new Set(["PRESTAMO", "COMODATO"]);

export const objetoMuseoSchema = z.object({
  numeroInventario: z
    .string()
    .trim()
    .min(1, "El numero de inventario es obligatorio")
    .max(80, "El numero de inventario no puede superar 80 caracteres"),
  denominacionObjeto: z
    .string()
    .trim()
    .min(1, "La denominacion es obligatoria")
    .max(160, "La denominacion no puede superar 160 caracteres"),
  descripcion: z.string().trim().optional().or(z.literal("")),
  descripcionTecnica: z.string().trim().optional().or(z.literal("")),
  materiales: z.string().trim().optional().or(z.literal("")),
  alto: z.string().trim().max(80, "El alto no puede superar 80 caracteres").optional().or(z.literal("")),
  ancho: z.string().trim().max(80, "El ancho no puede superar 80 caracteres").optional().or(z.literal("")),
  diametro: z.string().trim().max(80, "El diametro no puede superar 80 caracteres").optional().or(z.literal("")),
  espesor: z.string().trim().max(80, "El espesor no puede superar 80 caracteres").optional().or(z.literal("")),
  peso: z.string().trim().max(80, "El peso no puede superar 80 caracteres").optional().or(z.literal("")),
  inscripciones: z.string().trim().max(500, "Las inscripciones no pueden superar 500 caracteres").optional().or(z.literal("")),
  regimenPropiedad: z.enum(["", "PUBLICO", "PRIVADO"]),
  condicionLegalBien: z.string().trim().optional().or(z.literal("")),
  estadoConservacion: z.enum(["", "EXCELENTE", "BUENO", "REGULAR", "MALO", "CRITICO"]),
  detallesEstadoConservacion: z.array(z.string()).optional(),
  intervencionesInadecuadas: z.enum(["", "SI", "NO", "ELEMENTOS_EXTRANOS"]),
  estadoIntegridad: z.enum(["", "COMPLETO", "INCOMPLETO", "FRAGMENTADO"]),
  humedadConservacion: z.enum(["", "ALTA", "BAJA"]),
  temperaturaConservacion: z.string().trim().max(80, "La temperatura no puede superar 80 caracteres").optional().or(z.literal("")),
  luzConservacion: z.string().trim().max(80, "La luz no puede superar 80 caracteres").optional().or(z.literal("")),
  conservacionExtintores: z.enum(["", "true", "false"]),
  conservacionMontaje: z.enum(["", "true", "false"]),
  conservacionSistemaElectrico: z.enum(["", "true", "false"]),
  conservacionAlarmas: z.enum(["", "true", "false"]),
  conservacionCamaras: z.enum(["", "true", "false"]),
  visibilidades: z.record(z.string(), z.enum(["PUBLICO", "PRIVADO"])).optional(),
  categoriaIds: z.array(z.number()).optional(),
  ubicacionId: z.number().optional(),
  depositanteId: z.number().min(1, "El depositante es obligatorio"),
  caracterRecepcion: z.enum(["", "PRESTAMO", "COMODATO", "DONACION", "COMPRA", "ESTUDIO", "OTRO"]),
  fechaVencimiento: z.string().optional().or(z.literal(""))
}).superRefine((values, ctx) => {
  if (!values.caracterRecepcion) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, message: "El caracter de recepcion es obligatorio", path: ["caracterRecepcion"] });
  }
  if (caracteresConVencimiento.has(values.caracterRecepcion) && !values.fechaVencimiento) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, message: "La fecha de vencimiento es obligatoria", path: ["fechaVencimiento"] });
  }
});

export type ObjetoMuseoFormValues = z.infer<typeof objetoMuseoSchema>;

export const cargaRapidaObjetoSchema = z.object({
  depositanteId: z.number().min(1, "El depositante es obligatorio"),
  denominacionObjeto: z
    .string()
    .trim()
    .min(1, "La denominacion es obligatoria")
    .max(160, "La denominacion no puede superar 160 caracteres"),
  numeroInventario: z
    .string()
    .trim()
    .min(1, "El numero de inventario es obligatorio")
    .max(80, "El numero de inventario no puede superar 80 caracteres"),
  descripcionBreve: z.string().trim().min(5, "La descripcion breve debe tener al menos 5 caracteres")
});

export type CargaRapidaObjetoFormValues = z.infer<typeof cargaRapidaObjetoSchema>;
