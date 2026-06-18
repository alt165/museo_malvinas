import { z } from "zod";

export const coleccionObjetoSchema = z.object({
  nombre: z.string().trim().min(1, "El nombre es obligatorio").max(160, "El nombre no puede superar 160 caracteres"),
  descripcion: z.string().trim().optional().or(z.literal(""))
});

export type ColeccionObjetoFormValues = z.infer<typeof coleccionObjetoSchema>;
