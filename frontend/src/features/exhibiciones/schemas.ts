import { z } from "zod";
import { estadosExhibicion, tiposExhibicion } from "./types";

const today = new Date().toISOString().slice(0, 10);

export const exhibicionSchema = z
  .object({
    nombre: z.string().trim().min(1, "El nombre es obligatorio").max(160, "El nombre no puede superar 160 caracteres"),
    descripcion: z.string().trim().optional().or(z.literal("")),
    tipo: z.enum(tiposExhibicion, { message: "Selecciona un tipo" }),
    fechaInicio: z.string().min(1, "La fecha de inicio es obligatoria"),
    fechaFin: z.string().optional().or(z.literal("")),
    estado: z.enum(estadosExhibicion, { message: "Selecciona un estado" })
  })
  .refine((values) => !values.fechaFin || values.fechaFin >= values.fechaInicio, {
    path: ["fechaFin"],
    message: "La fecha de fin no puede ser anterior al inicio"
  });

export const agregarObjetoExhibicionSchema = z.object({
  objetoMuseoId: z.number().int("Selecciona un objeto").positive("Selecciona un objeto"),
  fechaInclusion: z
    .string()
    .min(1, "La fecha de inclusion es obligatoria")
    .refine((value) => value <= today, "La fecha de inclusion no puede ser futura")
});

export type ExhibicionFormValues = z.infer<typeof exhibicionSchema>;
export type AgregarObjetoExhibicionFormValues = z.infer<typeof agregarObjetoExhibicionSchema>;
