"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect } from "react";
import { useForm, useWatch } from "react-hook-form";
import type { DepositanteRequestDTO, DepositanteResponseDTO } from "../types";
import { tiposDepositante } from "../types";
import { depositanteSchema, type DepositanteFormValues } from "../schemas";
import { depositanteToFormValues, formValuesToDepositanteRequest, getValidationErrors } from "../utils";

type DepositanteFormProps = {
  initialValue?: DepositanteResponseDTO;
  isSubmitting?: boolean;
  submitError?: unknown;
  submitLabel: string;
  onSubmit: (payload: DepositanteRequestDTO) => void;
};

export function DepositanteForm({
  initialValue,
  isSubmitting = false,
  onSubmit,
  submitError,
  submitLabel
}: DepositanteFormProps) {
  const {
    formState: { errors },
    control,
    handleSubmit,
    register,
    setError
  } = useForm<DepositanteFormValues>({
    resolver: zodResolver(depositanteSchema),
    defaultValues: depositanteToFormValues(initialValue)
  });
  const tipo = useWatch({ control, name: "tipo" });

  useEffect(() => {
    const validationErrors = getValidationErrors(submitError);

    Object.entries(validationErrors).forEach(([field, message]) => {
      if (field === "tipo" || field === "nombre" || field === "contacto" || field === "observaciones") {
        const formField = field === "contacto" ? "email" : field;
        setError(formField, { message });
      }
    });
  }, [setError, submitError]);

  return (
    <form
      className="max-w-4xl space-y-5 rounded-lg border p-5"
      onSubmit={handleSubmit((values) => onSubmit(formValuesToDepositanteRequest(values)))}
    >
      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="Tipo" error={errors.tipo?.message}>
          <select
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            {...register("tipo")}
          >
            {tiposDepositante.map((tipoDepositante) => (
              <option key={tipoDepositante} value={tipoDepositante}>
                {tipoDepositante}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Email" error={errors.email?.message}>
          <input
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            type="email"
            {...register("email")}
          />
        </Field>
      </div>
      {tipo === "PERSONA" ? (
        <div className="grid gap-5 sm:grid-cols-2">
          <Field label="Nombre" error={errors.nombre?.message}>
            <input
              className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
              {...register("nombre")}
            />
          </Field>
          <Field label="Apellido" error={errors.apellido?.message}>
            <input
              className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
              {...register("apellido")}
            />
          </Field>
        </div>
      ) : (
        <Field label="Organizacion" error={errors.organizacion?.message}>
          <input
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            {...register("organizacion")}
          />
        </Field>
      )}
      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="Telefono" error={errors.telefono?.message}>
          <input
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            {...register("telefono")}
          />
        </Field>
        <Field label="Direccion" error={errors.direccion?.message}>
          <input
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            {...register("direccion")}
          />
        </Field>
      </div>
      <Field label="Observaciones" error={errors.observaciones?.message}>
        <textarea
          className="min-h-32 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
          {...register("observaciones")}
        />
      </Field>
      <div className="flex items-center gap-3">
        <button
          className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? "Guardando..." : submitLabel}
        </button>
        <Link className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm hover:bg-muted" href="/depositantes">
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
