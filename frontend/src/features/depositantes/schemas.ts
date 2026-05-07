import { z } from "zod";
import { tiposDepositante } from "./types";

export const depositanteSchema = z
  .object({
    tipo: z.enum(tiposDepositante, { message: "Selecciona un tipo de depositante" }),
    nombre: z.string().trim().max(160, "El nombre no puede superar 160 caracteres").optional().or(z.literal("")),
    apellido: z.string().trim().max(160, "El apellido no puede superar 160 caracteres").optional().or(z.literal("")),
    organizacion: z.string().trim().max(160, "La organizacion no puede superar 160 caracteres").optional().or(z.literal("")),
    email: z
      .string()
      .trim()
      .max(160, "El email no puede superar 160 caracteres")
      .email("El email debe tener un formato valido")
      .optional()
      .or(z.literal("")),
    telefono: z.string().trim().max(80, "El telefono no puede superar 80 caracteres").optional().or(z.literal("")),
    direccion: z.string().trim().max(255, "La direccion no puede superar 255 caracteres").optional().or(z.literal("")),
    observaciones: z.string().trim().optional().or(z.literal(""))
  })
  .superRefine((values, context) => {
    if (values.tipo === "PERSONA" && !values.nombre?.trim()) {
      context.addIssue({
        code: "custom",
        message: "El nombre es obligatorio",
        path: ["nombre"]
      });
    }

    if (values.tipo === "INSTITUCION" && !values.organizacion?.trim()) {
      context.addIssue({
        code: "custom",
        message: "La organizacion es obligatoria",
        path: ["organizacion"]
      });
    }
  });

export type DepositanteFormValues = z.infer<typeof depositanteSchema>;
