import { z } from "zod";

export const objetoMuseoSchema = z.object({
  numeroInventario: z
    .string()
    .trim()
    .min(1, "El numero de inventario es obligatorio")
    .max(80, "El numero de inventario no puede superar 80 caracteres"),
  nombre: z
    .string()
    .trim()
    .min(1, "El nombre es obligatorio")
    .max(160, "El nombre no puede superar 160 caracteres"),
  tipoObjeto: z
    .string()
    .trim()
    .max(100, "El tipo de objeto no puede superar 100 caracteres")
    .optional()
    .or(z.literal("")),
  descripcion: z.string().trim().optional().or(z.literal(""))
});

export type ObjetoMuseoFormValues = z.infer<typeof objetoMuseoSchema>;
