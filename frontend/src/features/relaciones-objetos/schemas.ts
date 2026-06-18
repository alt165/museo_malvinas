import { z } from "zod";

export const relacionObjetoSchema = z
  .object({
    objetoOrigenId: z.number().int("Selecciona un objeto origen").positive("Selecciona un objeto origen"),
    objetoDestinoId: z.number().int("Selecciona un objeto destino").positive("Selecciona un objeto destino"),
    tipoRelacion: z
      .string()
      .trim()
      .min(1, "El tipo de relacion es obligatorio")
      .max(80, "El tipo de relacion no puede superar 80 caracteres"),
    descripcion: z.string().trim().optional().or(z.literal(""))
  })
  .refine((values) => values.objetoOrigenId !== values.objetoDestinoId, {
    message: "El objeto origen y destino no pueden ser el mismo",
    path: ["objetoDestinoId"]
  });

export type RelacionObjetoFormValues = z.infer<typeof relacionObjetoSchema>;
