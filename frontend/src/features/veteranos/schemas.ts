import { z } from "zod";
import { fuerzas } from "./types";

const today = new Date().toISOString().slice(0, 10);

export const veteranoSchema = z.object({
  nombre: z.string().trim().min(1, "El nombre es obligatorio").max(100, "El nombre no puede superar 100 caracteres"),
  apellido: z.string().trim().min(1, "El apellido es obligatorio").max(100, "El apellido no puede superar 100 caracteres"),
  fuerza: z.enum(fuerzas, { message: "Selecciona una fuerza" }),
  fechaNacimiento: z.string().optional().or(z.literal("")).refine((value) => !value || value <= today, "La fecha de nacimiento no puede ser futura"),
  fechaFallecimiento: z.string().optional().or(z.literal("")).refine((value) => !value || value <= today, "La fecha de fallecimiento no puede ser futura"),
  historia: z.string().trim().optional().or(z.literal(""))
});

export const actuacionVeteranoSchema = z.object({
  rango: z.string().trim().max(80, "El rango no puede superar 80 caracteres").optional().or(z.literal("")),
  unidad: z.string().trim().max(120, "La unidad no puede superar 120 caracteres").optional().or(z.literal("")),
  rol: z.string().trim().max(120, "El rol no puede superar 120 caracteres").optional().or(z.literal("")),
  fechaInicio: z.string().optional().or(z.literal("")).refine((value) => !value || value <= today, "La fecha de inicio no puede ser futura"),
  fechaFin: z.string().optional().or(z.literal("")).refine((value) => !value || value <= today, "La fecha de fin no puede ser futura"),
  descripcion: z.string().trim().optional().or(z.literal(""))
});

export const objetoVeteranoSchema = z.object({
  objetoMuseoId: z.number().int("Selecciona un objeto").positive("Selecciona un objeto"),
  tipoRelacion: z.string().trim().min(1, "El tipo de relación es obligatorio").max(100, "El tipo de relación no puede superar 100 caracteres"),
  descripcion: z.string().trim().optional().or(z.literal(""))
});

export type VeteranoFormValues = z.infer<typeof veteranoSchema>;
export type ActuacionVeteranoFormValues = z.infer<typeof actuacionVeteranoSchema>;
export type ObjetoVeteranoFormValues = z.infer<typeof objetoVeteranoSchema>;
