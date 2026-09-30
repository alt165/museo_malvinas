import { z } from "zod";
import { tiposDepositante } from "./types";

function crearDepositanteSchema(esAlta: boolean) {
  return z
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
    dni: z.string().trim().max(30, "El DNI no puede superar 30 caracteres").optional().or(z.literal("")),
    cuit: z.string().trim().max(30, "El CUIT no puede superar 30 caracteres").optional().or(z.literal("")),
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

    if (!esAlta) {
      return;
    }

    const camposRequeridos = [
      ["email", "El email es obligatorio"],
      ["telefono", "El telefono es obligatorio"],
      ["direccion", "La direccion es obligatoria"]
    ] as const;

    for (const [campo, mensaje] of camposRequeridos) {
      if (!values[campo]?.trim()) {
        context.addIssue({ code: "custom", message: mensaje, path: [campo] });
      }
    }

    if (values.tipo === "PERSONA") {
      if (!values.apellido?.trim()) {
        context.addIssue({ code: "custom", message: "El apellido es obligatorio", path: ["apellido"] });
      }
      if (!values.dni?.trim()) {
        context.addIssue({ code: "custom", message: "El DNI es obligatorio", path: ["dni"] });
      }
    } else if (!values.cuit?.trim()) {
      context.addIssue({ code: "custom", message: "El CUIT es obligatorio", path: ["cuit"] });
    }
  });
}

export const depositanteSchema = crearDepositanteSchema(false);
export const altaDepositanteSchema = crearDepositanteSchema(true);

export type DepositanteFormValues = z.infer<typeof depositanteSchema>;
