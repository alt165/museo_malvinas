import { z } from "zod";

const roleSchema = z.enum(["ADMIN", "OPERATOR", "VIEWER"]);
const optionalText = z.string().trim().max(100).optional().or(z.literal(""));
const optionalPassword = z.string().optional().or(z.literal(""));

export const usuarioSchema = z.object({
  username: z.string().trim().min(1, "El nombre de usuario es obligatorio").max(100, "El nombre de usuario no puede superar 100 caracteres"),
  email: z.string().trim().min(1, "El email es obligatorio").email("El email debe tener un formato valido").max(160, "El email no puede superar 160 caracteres"),
  nombre: optionalText,
  apellido: optionalText,
  habilitado: z.boolean(),
  rol: roleSchema,
  contrasenaInicial: optionalPassword
});

export const usuarioCrearSchema = usuarioSchema.superRefine((values, context) => {
  if (values.contrasenaInicial && values.contrasenaInicial.length < 8) {
    context.addIssue({
      code: z.ZodIssueCode.custom,
      message: "La contrasena debe tener al menos 8 caracteres",
      path: ["contrasenaInicial"]
    });
  }
});

export const resetPasswordSchema = z.object({
  contrasena: z.string().min(8, "La contrasena debe tener al menos 8 caracteres").max(120, "La contrasena no puede superar 120 caracteres")
});

export type UsuarioFormValues = z.infer<typeof usuarioSchema>;
export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;
