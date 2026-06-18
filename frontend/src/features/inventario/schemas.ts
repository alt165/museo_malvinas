import { z } from "zod";
import { estadosConservacion, estadosInventario } from "./types";

const today = new Date().toISOString().slice(0, 10);

export const inventarioSchema = z.object({
  objetoMuseoId: z.number().int("Selecciona un objeto").positive("Selecciona un objeto"),
  ubicacionId: z.number().int("Selecciona una ubicacion").positive("Selecciona una ubicacion"),
  estado: z.enum(estadosInventario, {
    message: "Selecciona un estado"
  }),
  estadoConservacion: z.enum(estadosConservacion, {
    message: "Selecciona un estado de conservacion"
  }),
  fechaIngreso: z
    .string()
    .min(1, "La fecha de ingreso es obligatoria")
    .refine((value) => value <= today, "La fecha de ingreso no puede ser futura"),
  fechaSalida: z
    .string()
    .optional()
    .or(z.literal(""))
    .refine((value) => !value || value <= today, "La fecha de salida no puede ser futura"),
  observaciones: z.string().trim().optional().or(z.literal(""))
});

export type InventarioFormValues = z.infer<typeof inventarioSchema>;
