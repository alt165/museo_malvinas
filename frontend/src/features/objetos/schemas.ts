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
  dimensiones: z.string().trim().optional().or(z.literal("")),
  estadoConservacion: z.enum(["", "EXCELENTE", "BUENO", "REGULAR", "MALO", "CRITICO"]),
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
