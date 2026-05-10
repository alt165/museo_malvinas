"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import type { UserRole } from "@/models/session";
import { usuarioCrearSchema, usuarioSchema, type UsuarioFormValues } from "../schemas";
import { rolesUsuario, type UsuarioKeycloakRequestDTO, type UsuarioKeycloakResponseDTO } from "../types";
import { formValuesToUsuarioRequest, getValidationErrors, usuarioToFormValues } from "../utils";

type UsuarioFormPayload = {
  usuario: UsuarioKeycloakRequestDTO;
  rol: UserRole;
};

type UsuarioFormProps = {
  initialValue?: UsuarioKeycloakResponseDTO;
  includeInitialPassword?: boolean;
  isSubmitting?: boolean;
  submitError?: unknown;
  submitLabel: string;
  onSubmit: (payload: UsuarioFormPayload) => void;
};

export function UsuarioForm({
  includeInitialPassword = false,
  initialValue,
  isSubmitting = false,
  onSubmit,
  submitError,
  submitLabel
}: UsuarioFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register,
    setError
  } = useForm<UsuarioFormValues>({
    resolver: zodResolver(includeInitialPassword ? usuarioCrearSchema : usuarioSchema),
    defaultValues: usuarioToFormValues(initialValue)
  });

  useEffect(() => {
    const validationErrors = getValidationErrors(submitError);

    Object.entries(validationErrors).forEach(([field, message]) => {
      if (field === "username" || field === "email" || field === "dni" || field === "nombre" || field === "apellido" || field === "contrasenaInicial") {
        setError(field, { message });
      }
    });
  }, [setError, submitError]);

  return (
    <form
      className="max-w-4xl space-y-5 rounded-lg border p-5"
      onSubmit={handleSubmit((values) => {
        const usuario = formValuesToUsuarioRequest(values);
        onSubmit({ usuario, rol: values.rol });
      })}
    >
      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="Usuario" error={errors.username?.message}>
          <input
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            autoComplete="username"
            {...register("username")}
          />
        </Field>
        <Field label="Email" error={errors.email?.message}>
          <input
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            autoComplete="email"
            type="email"
            {...register("email")}
          />
        </Field>
      </div>
      <Field label="DNI" error={errors.dni?.message}>
        <input
          className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
          autoComplete="off"
          inputMode="numeric"
          {...register("dni")}
        />
      </Field>
      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="Nombre" error={errors.nombre?.message}>
          <input
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            autoComplete="given-name"
            {...register("nombre")}
          />
        </Field>
        <Field label="Apellido" error={errors.apellido?.message}>
          <input
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            autoComplete="family-name"
            {...register("apellido")}
          />
        </Field>
      </div>
      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="Rol" error={errors.rol?.message}>
          <select
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            {...register("rol")}
          >
            {rolesUsuario.map((rol) => (
              <option key={rol} value={rol}>
                {rol}
              </option>
            ))}
          </select>
        </Field>
        <label className="flex h-10 items-center gap-2 self-end text-sm font-medium">
          <input className="h-4 w-4 rounded border" type="checkbox" {...register("habilitado")} />
          Usuario habilitado
        </label>
      </div>
      {includeInitialPassword ? (
        <Field label="Contrasena inicial temporal" error={errors.contrasenaInicial?.message}>
          <input
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            autoComplete="new-password"
            type="password"
            {...register("contrasenaInicial")}
          />
        </Field>
      ) : null}
      <div className="flex items-center gap-3">
        <button
          className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? "Guardando..." : submitLabel}
        </button>
        <Link className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm hover:bg-muted" href="/usuarios">
          Cancelar
        </Link>
      </div>
    </form>
  );
}

function Field({ children, error, label }: { children: React.ReactNode; error?: string; label: string }) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      {children}
      {error ? <p className="font-normal text-destructive">{error}</p> : null}
    </label>
  );
}
