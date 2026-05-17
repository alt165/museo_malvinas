import { z } from "zod";

export const ubicacionSchema = z.object({
  nombre: z.string().trim().min(1, "El nombre es obligatorio").max(120, "El nombre no puede superar 120 caracteres"),
  tipo: z.string().trim().max(80, "El tipo no puede superar 80 caracteres").optional().or(z.literal("")),
  descripcion: z.string().trim().optional().or(z.literal(""))
});

export type UbicacionFormValues = z.infer<typeof ubicacionSchema>;
