import { z } from "zod";

export const categoriaSchema = z.object({
  nombre: z
    .string()
    .trim()
    .min(1, "El nombre es obligatorio")
    .max(100, "El nombre no puede superar 100 caracteres"),
  descripcion: z
    .string()
    .trim()
    .max(500, "La descripcion no puede superar 500 caracteres")
    .optional()
    .or(z.literal(""))
});

export type CategoriaFormValues = z.infer<typeof categoriaSchema>;
